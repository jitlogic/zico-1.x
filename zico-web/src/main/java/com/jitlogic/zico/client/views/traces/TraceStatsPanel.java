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
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.api.SystemService;
import com.jitlogic.zico.client.api.TraceDataService;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.views.Shell;
import com.jitlogic.zico.shared.data.*;
import com.jitlogic.zico.widgets.client.*;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

public class TraceStatsPanel extends Composite {

    interface TraceStatsPanelUiBinder extends UiBinder<Widget, TraceStatsPanel> { }

    private static TraceStatsPanelUiBinder ourUiBinder = GWT.create(TraceStatsPanelUiBinder.class);

    @UiField
    DockLayoutPanel panel;

    @UiField(provided = true)
    Resources resources;

    @UiField
    ListBox lstTraceType;

    @UiField
    ListBox lstTraceAttr;

    @UiField
    TextBox txtStartDate;

    @UiField
    TextBox txtEndDate;

    @UiField(provided = true)
    DataGrid<TraceInfoStatsResult> grid;

    private Provider<Shell> shell;

    private HostInfo host;

    private PanelFactory pf;

    private TraceDataService traceDataService;
    private SystemService systemService;

    private MessageDisplay md;

    private Map<Integer,Map<String,Integer>> traceAttrs;

    private SingleSelectionModel<TraceInfoStatsResult> selection;
    private ListDataProvider<TraceInfoStatsResult> data;
    private ColumnSortEvent.ListHandler<TraceInfoStatsResult> sortHandler;

    private PopupMenu contextMenu;

    private final String MDS;

    @Inject
    public TraceStatsPanel(Provider<Shell> shell, TraceDataService traceDataService, SystemService systemService,
                           PanelFactory pf, @Assisted HostInfo host, MessageDisplay md) {
        this.shell = shell;
        this.traceDataService = traceDataService;
        this.systemService = systemService;
        this.pf = pf;
        this.host = host;
        this.md = md;

        this.resources = Resources.INSTANCE;
        this.MDS = "TraceStats:" + host.getName();

        traceAttrs = new HashMap<Integer,Map<String,Integer>>();

        createTraceGrid();

        ourUiBinder.createAndBindUi(this);

        createContextMenu();

        initWidget(panel);

        loadTraceTypes();

        txtStartDate.setText(ClientUtil.TSTAMP_FORMAT0.format(new Date()) + " 00:00:00");

        Date d2 = new Date();
        d2.setTime(d2.getTime()+86400000);

        txtEndDate.setText(ClientUtil.TSTAMP_FORMAT0.format(d2) + " 00:00:00");
    }


    private void createTraceGrid() {
        grid = new DataGrid<TraceInfoStatsResult>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selection = new SingleSelectionModel<TraceInfoStatsResult>(KEY_PROVIDER);
        grid.setSelectionModel(selection);

        data = new ListDataProvider<TraceInfoStatsResult>();
        data.addDataDisplay(grid);

        sortHandler = new ColumnSortEvent.ListHandler<TraceInfoStatsResult>(data.getList());
        grid.addColumnSortHandler(sortHandler);

        Column<TraceInfoStatsResult, TraceInfoStatsResult> colTraceCalls
                = new IdentityColumn<TraceInfoStatsResult>(TRACE_CALLS_CELL);
        grid.addColumn(colTraceCalls, new ResizableHeader<TraceInfoStatsResult>("Calls", grid, colTraceCalls));
        grid.setColumnWidth(colTraceCalls, 50, Style.Unit.PX);

        colTraceCalls.setSortable(true);
        sortHandler.setComparator(colTraceCalls, new Comparator<TraceInfoStatsResult>() {
            @Override
            public int compare(TraceInfoStatsResult o1, TraceInfoStatsResult o2) {
                return o1.getCalls()-o2.getCalls();
            }
        });

        Column<TraceInfoStatsResult, TraceInfoStatsResult> colTraceErrors
                = new IdentityColumn<TraceInfoStatsResult>(TRACE_ERRORS_CELL);
        grid.addColumn(colTraceErrors, new ResizableHeader<TraceInfoStatsResult>("Errors", grid, colTraceErrors));
        grid.setColumnWidth(colTraceErrors, 50, Style.Unit.PX);

        colTraceErrors.setSortable(true);
        sortHandler.setComparator(colTraceErrors, new Comparator<TraceInfoStatsResult>() {
            @Override
            public int compare(TraceInfoStatsResult o1, TraceInfoStatsResult o2) {
                return o1.getErrors()-o2.getErrors();
            }
        });

        Column<TraceInfoStatsResult, TraceInfoStatsResult> colSumTime
                = new IdentityColumn<TraceInfoStatsResult>(TIME_SUM_CELL);
        grid.addColumn(colSumTime, new ResizableHeader<TraceInfoStatsResult>("Sum Time", grid, colSumTime));
        grid.setColumnWidth(colSumTime, 96, Style.Unit.PX);

        colSumTime.setSortable(true);
        sortHandler.setComparator(colSumTime, new Comparator<TraceInfoStatsResult>() {
            @Override
            public int compare(TraceInfoStatsResult o1, TraceInfoStatsResult o2) {
                return (int)((o1.getSumTime()-o2.getSumTime())/1000000L);
            }
        });

        // avgTime

        Column<TraceInfoStatsResult, TraceInfoStatsResult> colMinTime
                = new IdentityColumn<TraceInfoStatsResult>(TIME_MIN_CELL);
        grid.addColumn(colMinTime, new ResizableHeader<TraceInfoStatsResult>("Min Time", grid, colMinTime));
        grid.setColumnWidth(colMinTime, 96, Style.Unit.PX);

        colMinTime.setSortable(true);
        sortHandler.setComparator(colMinTime, new Comparator<TraceInfoStatsResult>() {
            @Override
            public int compare(TraceInfoStatsResult o1, TraceInfoStatsResult o2) {
                return (int)((o1.getMinTime()-o2.getMinTime())/1000000L);
            }
        });

        Column<TraceInfoStatsResult, TraceInfoStatsResult> colMaxTime
                = new IdentityColumn<TraceInfoStatsResult>(TIME_MAX_CELL);
        grid.addColumn(colMaxTime, new ResizableHeader<TraceInfoStatsResult>("Max Time", grid, colMaxTime));
        grid.setColumnWidth(colMaxTime, 96, Style.Unit.PX);

        colMaxTime.setSortable(true);
        sortHandler.setComparator(colMaxTime, new Comparator<TraceInfoStatsResult>() {
            @Override
            public int compare(TraceInfoStatsResult o1, TraceInfoStatsResult o2) {
                return (int)((o1.getMaxTime()-o2.getMaxTime())/1000000L);
            }
        });

        Column<TraceInfoStatsResult, TraceInfoStatsResult> colAttr
                = new IdentityColumn<TraceInfoStatsResult>(ATTR_CELL);
        colAttr.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        grid.addColumn(colAttr, new ResizableHeader<TraceInfoStatsResult>("Attr", grid, colAttr));
        grid.setColumnWidth(colAttr, 100, Style.Unit.PCT);

        colAttr.setSortable(true);
        sortHandler.setComparator(colAttr, new Comparator<TraceInfoStatsResult>() {
            @Override
            public int compare(TraceInfoStatsResult o1, TraceInfoStatsResult o2) {
                return o1.getAttr().compareTo(o2.getAttr());
            }
        });

        grid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceInfoStatsResult>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceInfoStatsResult> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selection.setSelected(event.getValue(), true);
                    openSearchView();
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

        com.jitlogic.zico.widgets.client.MenuItem mnuMethodTree = new com.jitlogic.zico.widgets.client.MenuItem("Show individual traces", Resources.INSTANCE.methodTreeIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        openSearchView();
                    }
                });
        contextMenu.addItem(mnuMethodTree);
    }


    @UiHandler("lstTraceType")
    void onTraceTypeChange(ChangeEvent ev) {
        lstTraceAttr.clear();
        int id = Integer.parseInt(lstTraceType.getValue(lstTraceType.getSelectedIndex()));
        Map<String,Integer> attrs = traceAttrs.get(id);
        if (attrs != null) {
            List<String> names = new ArrayList<String>();
            names.addAll(attrs.keySet());
            Collections.sort(names);
            lstTraceAttr.addItem("<select attribute>", "0");
            for (String name : names) {
                lstTraceAttr.addItem(name, attrs.get(name).toString());
            }
        }
    }

    @UiHandler("lstTraceAttr")
    void onTraceAttrChange(ChangeEvent ev) {
        refresh(null);
    }

    @UiHandler("btnRefresh")
    void refresh(ClickEvent e) {

        if (lstTraceAttr.getSelectedIndex() == -1 || lstTraceType.getSelectedIndex() == -1) {
            return;
        }

        int traceId = Integer.parseInt(lstTraceType.getValue(lstTraceType.getSelectedIndex()));
        int attrId  = Integer.parseInt(lstTraceAttr.getValue(lstTraceAttr.getSelectedIndex()));

        Date startDate = ClientUtil.parseDate(txtStartDate.getText());
        Date endDate = ClientUtil.parseDate(txtEndDate.getText());

        if (traceId > 0 && attrId > 0 && startDate != null && endDate != null) {
            TraceInfoStatsQuery q = new TraceInfoStatsQuery();
            q.setHostName(host.getName());
            q.setTraceId(traceId);
            q.setAttrId(attrId);
            q.setStartClock(startDate.getTime());
            q.setEndClock(endDate.getTime());

            traceDataService.statTraces(q, new MethodCallback<List<TraceInfoStatsResult>>() {
                @Override
                public void onFailure(Method method, Throwable e) {
                    md.error(MDS, "Error calling for trace pivot data", e);
                }

                @Override
                public void onSuccess(Method method, List<TraceInfoStatsResult> response) {
                    data.getList().clear();
                    data.getList().addAll(response);
                    grid.redraw();
                }
            });
        }
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
                refresh(null);
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
                refresh(null);
            }
        });

        dtp.show();
    }


    private void openSearchView() {
        // TODO open search view and run search with appropriate arguments
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
                lstTraceType.addItem("<select>");
                for (SymbolInfo e : response) {
                    lstTraceType.addItem(e.getName(), "" + e.getId());
                }
                lstTraceType.setEnabled(true);
            }
        });
        traceDataService.attrNames(host.getName(), new MethodCallback<Map<Integer, Map<String,Integer>>>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error loading attribute names", e);
            }

            @Override
            public void onSuccess(Method method, Map<Integer, Map<String,Integer>> response) {
                traceAttrs = response;
                lstTraceAttr.setEnabled(true);
            }
        });
    }

    private final static String SMALL_CELL_CSS = Resources.INSTANCE.zicoCssResources().traceSmallCell();


    private static final ProvidesKey<TraceInfoStatsResult> KEY_PROVIDER = new ProvidesKey<TraceInfoStatsResult>() {
        @Override
        public Object getKey(TraceInfoStatsResult item) {
            return item.getAttr();
        }
    };


    private AbstractCell<TraceInfoStatsResult> TRACE_CALLS_CELL = new AbstractCell<TraceInfoStatsResult>() {
        @Override
        public void render(Context context, TraceInfoStatsResult rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getCalls()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoStatsResult> TRACE_ERRORS_CELL = new AbstractCell<TraceInfoStatsResult>() {
        @Override
        public void render(Context context, TraceInfoStatsResult rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getErrors()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoStatsResult> TIME_SUM_CELL = new AbstractCell<TraceInfoStatsResult>() {
        @Override
        public void render(Context context, TraceInfoStatsResult rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getSumTime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoStatsResult> TIME_MIN_CELL = new AbstractCell<TraceInfoStatsResult>() {
        @Override
        public void render(Context context, TraceInfoStatsResult rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getMinTime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoStatsResult> TIME_MAX_CELL = new AbstractCell<TraceInfoStatsResult>() {
        @Override
        public void render(Context context, TraceInfoStatsResult rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getMaxTime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoStatsResult> ATTR_CELL = new AbstractCell<TraceInfoStatsResult>() {
        @Override
        public void render(Context context, TraceInfoStatsResult rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getAttr()));
            sb.appendHtmlConstant("</div>");
        }
    };

}