/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.client.views.traces;


import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.api.SystemService;
import com.jitlogic.zico.client.api.TraceDataService;
import com.jitlogic.zico.client.views.Shell;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.shared.data.*;
import com.jitlogic.zico.widgets.client.*;
import com.jitlogic.zico.widgets.client.MenuItem;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;


public class TraceSearchPanel extends Composite {
    interface TraceSearchPanelUiBinder extends UiBinder<Widget, TraceSearchPanel> { }
    private static TraceSearchPanelUiBinder ourUiBinder = GWT.create(TraceSearchPanelUiBinder.class);

    @UiField
    DockLayoutPanel panel;

    @UiField(provided = true)
    Resources resources;

    @UiField
    ToolButton btnDeepSearch;

    @UiField
    ToolButton btnErrors;

    @UiField
    ListBox lstTraceType;

    @UiField
    TextBox txtDuration;

    @UiField
    ToolButton btnEnableEql;

    @UiField
    TextBox txtFilter;

    @UiField
    TextBox txtStartDate;

    @UiField
    TextBox txtEndDate;

    @UiField
    ToolButton btnRunSearch;

    @UiField
    ToolButton btnFindMore;

    @UiField
    ToolButton btnClearFilters;

    @UiField(provided = true)
    DataGrid<TraceInfo> grid;

    public final static String RE_TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}\\s*(\\d{2}:\\d{2}:\\d{2}(\\.\\d{1-3})?)?";

    private PanelFactory pf;

    private TraceDataService traceDataService;
    private SystemService systemService;

    private Provider<Shell> shell;

    private HostInfo host;

    private ListDataProvider<TraceInfo> data;
    private SingleSelectionModel<TraceInfo> selection;
    private ColumnSortEvent.ListHandler<TraceInfo> sortHandler;

    private TraceSearchTableBuilder rowBuilder;
    private Set<Long> expandedDetails = new HashSet<Long>();

    private int seqnum = 0;

    private PopupMenu contextMenu;
    private boolean moreResults;

    private String strTraceType;

    private MessageDisplay md;
    private final String MDS;


    @Inject
    public TraceSearchPanel(Provider<Shell> shell, TraceDataService traceDataService, SystemService systemService,
                            PanelFactory pf, @Assisted HostInfo host, MessageDisplay md,
                            @Assisted String traceName) {
        this.shell = shell;
        this.traceDataService = traceDataService;
        this.systemService = systemService;
        this.pf = pf;
        this.host = host;
        this.md = md;
        this.MDS = "TraceSearch:" + host.getName();

        this.resources = Resources.INSTANCE;


        createTraceGrid();

        ourUiBinder.createAndBindUi(this);

        createContextMenu();

        initWidget(panel);

        btnFindMore.setEnabled(false);

        if (traceName != null) {
            lstTraceType.addItem(traceName);
            lstTraceType.setSelectedIndex(0);
        } else {
            loadTraceTypes();
        }


    }


    @UiHandler("lstTraceType")
    void onTraceTypeChange(ChangeEvent e) {
        strTraceType = lstTraceType.getItemText(lstTraceType.getSelectedIndex());
        refresh();
    }

    @UiHandler({"txtDuration", "txtFilter", "txtStartDate"})
    void onTapEnter(KeyDownEvent e) {
        if (e.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            refresh();
        }
    }

    @UiHandler("btnRunSearch")
    void onSearchClick(ClickEvent e) {
        refresh();
    }

    @UiHandler("btnClearFilters")
    void onClearClick(ClickEvent e) {
        txtFilter.setText("");
        txtDuration.setText("");
        btnErrors.setToggled(false);
        strTraceType = null;
        refresh();
    }

    private void createTraceGrid() {
        grid = new DataGrid<TraceInfo>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selection = new SingleSelectionModel<TraceInfo>(KEY_PROVIDER);
        grid.setSelectionModel(selection);

        data = new ListDataProvider<TraceInfo>();
        data.addDataDisplay(grid);

        sortHandler = new ColumnSortEvent.ListHandler<TraceInfo>(data.getList());
        grid.addColumnSortHandler(sortHandler);

        Column<TraceInfo, TraceInfo> colExpander
                = new IdentityColumn<TraceInfo>(DETAIL_EXPANDER_CELL);
        grid.addColumn(colExpander, "#");
        grid.setColumnWidth(colExpander, 32, Style.Unit.PX);

        Column<TraceInfo, TraceInfo> colTraceClock
                = new IdentityColumn<TraceInfo>(TRACE_CLOCK_CELL);
        grid.addColumn(colTraceClock, new ResizableHeader<TraceInfo>("Clock", grid, colTraceClock));
        grid.setColumnWidth(colTraceClock, 140, Style.Unit.PX);

        colTraceClock.setSortable(true);
        sortHandler.setComparator(colTraceClock, new Comparator<TraceInfo>() {
            @Override
            public int compare(TraceInfo o1, TraceInfo o2) {
                return (int)(o1.getClock()-o2.getClock());
            }
        });

        Column<TraceInfo, TraceInfo> colTraceType
                = new IdentityColumn<TraceInfo>(TRACE_TYPE_CELL);
        grid.addColumn(colTraceType, new ResizableHeader<TraceInfo>("Type", grid, colTraceType));
        grid.setColumnWidth(colTraceType, 60, Style.Unit.PX);

        Column<TraceInfo, TraceInfo> colTraceDuration
                = new IdentityColumn<TraceInfo>(TRACE_DURATION_CELL);
        grid.addColumn(colTraceDuration, new ResizableHeader<TraceInfo>("Time", grid, colTraceDuration));
        grid.setColumnWidth(colTraceDuration, 64, Style.Unit.PX);
        colTraceDuration.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

        colTraceDuration.setSortable(true);
        sortHandler.setComparator(colTraceDuration, new Comparator<TraceInfo>() {
            @Override
            public int compare(TraceInfo o1, TraceInfo o2) {
                return (int)((o1.getExecutionTime()-o2.getExecutionTime())/1000000L);
            }
        });

        Column<TraceInfo, TraceInfo> colTraceCalls
                = new IdentityColumn<TraceInfo>(TRACE_CALLS_CELL);
        grid.addColumn(colTraceCalls, new ResizableHeader<TraceInfo>("Calls", grid, colTraceCalls));
        grid.setColumnWidth(colTraceCalls, 50, Style.Unit.PX);

        colTraceCalls.setSortable(true);
        sortHandler.setComparator(colTraceCalls, new Comparator<TraceInfo>() {
            @Override
            public int compare(TraceInfo o1, TraceInfo o2) {
                return (int)(o1.getCalls()-o2.getCalls());
            }
        });

        Column<TraceInfo, TraceInfo> colTraceErrors
                = new IdentityColumn<TraceInfo>(TRACE_ERRORS_CELL);
        grid.addColumn(colTraceErrors, new ResizableHeader<TraceInfo>("Errs", grid, colTraceErrors));
        grid.setColumnWidth(colTraceErrors, 50, Style.Unit.PX);

        colTraceErrors.setSortable(true);
        sortHandler.setComparator(colTraceErrors, new Comparator<TraceInfo>() {
            @Override
            public int compare(TraceInfo o1, TraceInfo o2) {
                return (int)(o1.getErrors()-o2.getErrors());
            }
        });

        Column<TraceInfo, TraceInfo> colTraceRecords
                = new IdentityColumn<TraceInfo>(TRACE_RECORDS_CELL);
        grid.addColumn(colTraceRecords, new ResizableHeader<TraceInfo>("Recs", grid, colTraceRecords));
        grid.setColumnWidth(colTraceRecords, 50, Style.Unit.PX);

        colTraceRecords.setSortable(true);
        sortHandler.setComparator(colTraceRecords, new Comparator<TraceInfo>() {
            @Override
            public int compare(TraceInfo o1, TraceInfo o2) {
                return (int)(o1.getRecords()-o2.getRecords());
            }
        });

        Column<TraceInfo, TraceInfo> colTraceDesc
                = new IdentityColumn<TraceInfo>(TRACE_NAME_CELL);
        grid.addColumn(colTraceDesc, "Description");
        grid.setColumnWidth(colTraceDesc, 100, Style.Unit.PCT);

        rowBuilder = new TraceSearchTableBuilder(grid, expandedDetails);
        grid.setTableBuilder(rowBuilder);

        grid.setSkipRowHoverStyleUpdate(true);
        grid.setSkipRowHoverFloatElementCheck(true);
        grid.setSkipRowHoverCheck(true);
        grid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        grid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceInfo>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceInfo> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selection.setSelected(event.getValue(), true);
                    openDetailView();
                }
                if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                    selection.setSelected(event.getValue(), true);
                    if (selection.getSelectedObject() != null) {
                        contextMenu.setPopupPosition(
                                event.getNativeEvent().getClientX(),
                                event.getNativeEvent().getClientY());
                        contextMenu.show();
                    }
                }
            }
        });

        grid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());
        grid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());
    }

    private void createContextMenu() {
        contextMenu = new PopupMenu();

        MenuItem mnuMethodTree = new MenuItem("Method call tree", Resources.INSTANCE.methodTreeIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        openDetailView();
                    }
                });
        contextMenu.addItem(mnuMethodTree);

        MenuItem mnuMethodRank = new MenuItem("Method call stats", Resources.INSTANCE.methodRankIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        openRankingView();
                    }
                });
        contextMenu.addItem(mnuMethodRank);

        contextMenu.addSeparator();

        MenuItem mnuMethodAttrs = new MenuItem("Trace Attributes", Resources.INSTANCE.methodAttrsIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        openMethodAttrsDialog();
                    }
                });
        contextMenu.addItem(mnuMethodAttrs);
    }


    private void intoSearchMode(boolean inSearch, final boolean moreResults, String message) {
        seqnum++;
        btnDeepSearch.setEnabled(!inSearch);
        btnErrors.setEnabled(!inSearch);
        lstTraceType.setEnabled(!inSearch);
        txtDuration.setEnabled(!inSearch);
        btnEnableEql.setEnabled(!inSearch);
        txtFilter.setEnabled(!inSearch);
        btnRunSearch.setEnabled(!inSearch);
        btnClearFilters.setEnabled(!inSearch);
        btnFindMore.setEnabled(!inSearch && moreResults);

        if (inSearch) {
            md.info(MDS, message, "Cancel",
                    new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            intoSearchMode(false, moreResults, "Search canceled.");
                        }
                    }
            );
        } else if (moreResults) {
            md.info(MDS, message, "More",
                    new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            loadMore(100);
                        }
                    });
        } else {
            md.info(MDS, message);
        }
    }


    private void openDetailView() {
        TraceInfo traceInfo = selection.getSelectedObject();
        if (traceInfo != null) {
            TraceCallTreePanel detail = pf.traceCallTreePanel(traceInfo);
            shell.get().addView(detail, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + host.getName());
        }
    }


    private void openRankingView() {
        TraceInfo traceInfo = selection.getSelectedObject();
        if (traceInfo != null) {
            MethodRankingPanel ranking = pf.methodRankingPanel(traceInfo);
            shell.get().addView(ranking, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + host.getName());
        }
    }

    private void openMethodAttrsDialog() {
        TraceInfo ti = selection.getSelectedObject();
        if (ti != null) {
            pf.methodAttrsDialog(ti.getHostName(), ti.getDataOffs(), "", 0L).asPopupWindow().show();
        }
    }

    public void refresh() {
        data.getList().clear();
        expandedDetails.clear();
        loadMore();
    }

    public void runSearch(String attrName, String filter, Date startDate, Date endDate) {
        txtFilter.setValue(filter, false);
        txtStartDate.setValue(ClientUtil.TSTAMP_FORMAT1.format(startDate), false);
        txtEndDate.setValue(ClientUtil.TSTAMP_FORMAT1.format(endDate), false);
        refresh();
    }

    @UiHandler("btnFindMore")
    void findMoreClicked(ClickEvent e) {
        loadMore();
    }

    @UiHandler("btnStartDate")
    void setStartDate(ClickEvent e) {
        final DateTimePicker dtp = new DateTimePicker(true);

        if (txtStartDate.getText().length() > 0) {
            dtp.setValue(ClientUtil.parseDate(txtStartDate.getValue()));
        } else {
            Date dt = new Date();
            dt.setTime(dt.getTime()-86400000L);
            dtp.setValue(dt);
        }

        dtp.setPopupPosition(Window.getClientWidth()-384, e.getClientY());

        dtp.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(ValueChangeEvent<Date> event) {
                txtStartDate.setValue(ClientUtil.TSTAMP_FORMAT1.format(event.getValue()));
                dtp.hide();
                refresh();
            }
        });

        dtp.show();
    }

    @UiHandler("btnEndDate")
    void setEndDate(ClickEvent e) {
        final DateTimePicker dtp = new DateTimePicker(true);

        if (txtEndDate.getText().length() > 0) {
            dtp.setValue(ClientUtil.parseDate(txtEndDate.getValue()));
        } else {
            Date dt = new Date();
            dtp.setValue(dt);
        }

        dtp.setPopupPosition(Window.getClientWidth()-256, e.getClientY());

        dtp.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(ValueChangeEvent<Date> event) {
                txtEndDate.setValue(ClientUtil.TSTAMP_FORMAT1.format(event.getValue()));
                dtp.hide();
                refresh();
            }
        });

        dtp.show();
    }

    private void loadMore() {
        loadMore(50);
    }

    private void loadMore(final int limit) {
        intoSearchMode(true, false, "Searching ...");
        TraceInfoSearchQuery q = new TraceInfoSearchQuery();
        q.setLimit(limit);
        q.setHostName(host.getName());
        q.setSeq(seqnum);

        q.setFlags(TraceInfoSearchQuery.ORDER_DESC |
                (btnErrors.isToggled() ? TraceInfoSearchQuery.ERRORS_ONLY : 0)
              | (btnDeepSearch.isToggled() ? TraceInfoSearchQuery.DEEP_SEARCH : 0)
              | (btnEnableEql.isToggled() ? TraceInfoSearchQuery.EQL_QUERY : 0)
        );

        List<TraceInfo> list = data.getList();
        if (list.size() > 0) {
            q.setOffset(list.get(list.size()-1).getDataOffs());
        }

        if (strTraceType != null && !"<all>".equals(strTraceType)) {
            q.setTraceName(strTraceType);
        }

        if (txtFilter.getText() != null && txtFilter.getText().length() > 0) {
            q.setSearchExpr(txtFilter.getText());
        }


        if (txtStartDate.getText() != null) {
            q.setStartDate(ClientUtil.parseTimestamp(txtStartDate.getText()));
        }

        if (txtEndDate.getText() != null) {
            q.setEndDate(ClientUtil.parseTimestamp(txtEndDate.getText()));
        }

        if (txtDuration.getText() != null && txtDuration.getText().length() > 0) {
            q.setMinMethodTime((Integer.parseInt(txtDuration.getText()) * 1000000000L));
        }

        md.info(MDS, "Searching for traces ...");
        traceDataService.search(q, new MethodCallback<TraceInfoSearchResult>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                intoSearchMode(false, false, "Error occured while searching: " + e.getMessage());
                md.error(MDS, "Trace search request failed", e);
            }

            @Override
            public void onSuccess(Method method, TraceInfoSearchResult response) {
                if (response.getSeq() == seqnum) {
                    List<TraceInfo> results = response.getResults();
                    data.getList().addAll(results);
                    moreResults = 0 != (response.getFlags() & TraceInfoSearchResult.MORE_RESULTS);
                    intoSearchMode(false, moreResults, "Found " + data.getList().size() + " results.");
                    if (moreResults && results.size() < limit) {
                        loadMore(limit - results.size());
                    }
                    md.info(MDS, "Found: " + results.size() + " traces " +
                            (moreResults ? "(more to come - click 'Find More' button)." : "."));
                } else {
                    intoSearchMode(false, false, "No more records found.");
                }
            }
        });
    }

    private void loadTraceTypes() {
        systemService.getTidMap(host.getName(), new MethodCallback<List<SymbolInfo>>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error loading TID map", e);
            }

            @Override
            public void onSuccess(Method method, List<SymbolInfo> response) {
                lstTraceType.clear();
                lstTraceType.addItem("<all>");
                for (SymbolInfo e : response) {
                    lstTraceType.addItem(e.getName());
                }
            }
        });
    }

    private void toggleDetails(TraceInfo ti) {
        long offs = ti.getDataOffs();
        if (expandedDetails.contains(offs)) {
            expandedDetails.remove(offs);
        } else {
            expandedDetails.add(offs);
        }
        grid.redrawRow(data.getList().indexOf(ti));
    }

    private static final ProvidesKey<TraceInfo> KEY_PROVIDER = new ProvidesKey<TraceInfo>() {
        @Override
        public Object getKey(TraceInfo item) {
            return item.getDataOffs();
        }
    };

    private final static String SMALL_CELL_CSS = Resources.INSTANCE.zicoCssResources().traceSmallCell();
    private final static String SMALL_CELL_CSS_R = Resources.INSTANCE.zicoCssResources().traceSmallCellR();
    private static final String EXPANDER_EXPAND = AbstractImagePrototype.create(Resources.INSTANCE.expanderExpand()).getHTML();
    private static final String EXPANDER_COLLAPSE = AbstractImagePrototype.create(Resources.INSTANCE.expanderCollapse()).getHTML();

    private final Cell<TraceInfo> DETAIL_EXPANDER_CELL = new ActionCell<TraceInfo>("",
            new ActionCell.Delegate<TraceInfo>() {
                @Override
                public void execute(TraceInfo rec) {
                    toggleDetails(rec);
                }
            }) {
        @Override
        public void render(Cell.Context context, TraceInfo tr, SafeHtmlBuilder sb) {
            if ((tr.getAttributes() != null && tr.getAttributes().size() > 0)||tr.getExceptionInfo() != null) {
                sb.appendHtmlConstant("<span style=\"cursor: pointer;\">");
                sb.appendHtmlConstant(expandedDetails.contains(tr.getDataOffs()) ? EXPANDER_COLLAPSE : EXPANDER_EXPAND);
                sb.appendHtmlConstant("</span>");
            }
        }
    };

    private AbstractCell<TraceInfo> TRACE_NAME_CELL = new AbstractCell<TraceInfo>() {
        @Override
        public void render(Context context, TraceInfo ti, SafeHtmlBuilder sb) {
            String color = ti.getStatus() != 0 ? "red" : "black";
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\" style=\"color: " + color + "; text-align: left;\">");
            sb.append(SafeHtmlUtils.fromString(ti.getDescription()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfo> TRACE_CLOCK_CELL = new AbstractCell<TraceInfo>() {
        @Override
        public void render(Context context, TraceInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatTimestamp(rec.getClock())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfo> TRACE_TYPE_CELL = new AbstractCell<TraceInfo>() {
        @Override
        public void render(Context context, TraceInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getTraceType()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfo> TRACE_DURATION_CELL = new AbstractCell<TraceInfo>() {
        @Override
        public void render(Context context, TraceInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS_R + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getExecutionTime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfo> TRACE_CALLS_CELL = new AbstractCell<TraceInfo>() {
        @Override
        public void render(Context context, TraceInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS_R + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getCalls()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfo> TRACE_RECORDS_CELL = new AbstractCell<TraceInfo>() {
        @Override
        public void render(Context context, TraceInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS_R + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getRecords()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfo> TRACE_ERRORS_CELL = new AbstractCell<TraceInfo>() {
        @Override
        public void render(Context context, TraceInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS_R + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getErrors()));
            sb.appendHtmlConstant("</div>");
        }
    };


}
