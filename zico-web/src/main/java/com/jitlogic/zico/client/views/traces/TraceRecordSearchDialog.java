/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zico.client.*;
import com.jitlogic.zico.client.api.TraceDataService;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.shared.data.TraceRecordSearchQuery;
import com.jitlogic.zico.shared.data.TraceInfo;
import com.jitlogic.zico.shared.data.TraceRecordInfo;
import com.jitlogic.zico.shared.data.TraceRecordSearchResult;
import com.jitlogic.zico.widgets.client.*;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class TraceRecordSearchDialog implements IsPopupWindow {
    interface TraceRecordSearchViewUiBinder extends UiBinder<Widget, TraceRecordSearchDialog> { }
    private static TraceRecordSearchViewUiBinder ourUiBinder = GWT.create(TraceRecordSearchViewUiBinder.class);

    @UiField
    CheckBox chkEql;

    @UiField
    TextBox txtSearchFilter;

    @UiField
    CheckBox chkClass;

    @UiField
    CheckBox chkMethod;

    @UiField
    CheckBox chkAttribs;

    @UiField
    CheckBox chkExceptionText;

    @UiField
    CheckBox chkErrorsOnly;

    @UiField
    CheckBox chkMethodsWithAttrs;

    @UiField
    CheckBox chkIgnoreCase;

    @UiField
    Label lblSumStats;

    @UiField(provided = true)
    DataGrid<TraceRecordInfo> resultsGrid;

    private ListDataProvider<TraceRecordInfo> resultsStore;
    private TraceCallTableBuilder rowBuilder;
    private SingleSelectionModel<TraceRecordInfo> selectionModel;
    private Set<String> expandedDetails = new HashSet<String>();

    private TraceInfo trace;
    private String rootPath = "";

    private TraceCallTreePanel panel;

    private TraceDataService traceDataService;

    private PopupWindow window;

    private MessageDisplay md;
    private final String MDS;

    @Inject
    public TraceRecordSearchDialog(TraceDataService traceDataService, MessageDisplay md,
                                   @Assisted TraceCallTreePanel panel, @Assisted TraceInfo trace) {

        this.traceDataService = traceDataService;
        this.trace = trace;
        this.panel = panel;
        this.md = md;

        this.MDS = "TraceRecordSearch:" + trace.getHostName() + ":" + trace.getDataOffs();

        createResultsGrid();

        window = new PopupWindow(ourUiBinder.createAndBindUi(this));
        window.setCaption("Search for methods");
        window.resizeAndCenter(900, 600);

        txtSearchFilter.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    doSearch();
                }
            }
        });

        chkClass.setValue(true);
        chkMethod.setValue(true);
        chkAttribs.setValue(true);
        chkIgnoreCase.setValue(true);
    }

    @UiHandler("btnSearch")
    void clickSearch(ClickEvent e) {
        doSearch();
    }

    private static final ProvidesKey<TraceRecordInfo> KEY_PROVIDER = new ProvidesKey<TraceRecordInfo>() {
        @Override
        public Object getKey(TraceRecordInfo rec) {
            return rec.getPath();
        }
    };

    private static final String EXPANDER_EXPAND = AbstractImagePrototype.create(Resources.INSTANCE.expanderExpand()).getHTML();
    private static final String EXPANDER_COLLAPSE = AbstractImagePrototype.create(Resources.INSTANCE.expanderCollapse()).getHTML();

    private final Cell<TraceRecordInfo> DETAIL_EXPANDER_CELL = new ActionCell<TraceRecordInfo>("",
            new ActionCell.Delegate<TraceRecordInfo>() {
                @Override
                public void execute(TraceRecordInfo rec) {
                    toggleDetails(rec);
                }
            }) {
        @Override
        public void render(Cell.Context context, TraceRecordInfo tr, SafeHtmlBuilder sb) {
            if ((tr.getAttributes() != null && tr.getAttributes().size() > 0)||tr.getExceptionInfo() != null) {
                sb.appendHtmlConstant("<span style=\"cursor: pointer;\">");
                sb.appendHtmlConstant(expandedDetails.contains(tr.getPath()) ? EXPANDER_COLLAPSE : EXPANDER_EXPAND);
                sb.appendHtmlConstant("</span>");
            }
        }
    };

    private AbstractCell<TraceRecordInfo> METHOD_CELL = new AbstractCell<TraceRecordInfo>() {
        @Override
        public void render(Context context, TraceRecordInfo tr, SafeHtmlBuilder sb) {
            String color = tr.getExceptionInfo() != null ? "red"
                    : tr.getAttributes() != null ? "blue" : "black";
            sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
            sb.append(SafeHtmlUtils.fromString(tr.getMethod()));
            sb.appendHtmlConstant("</span>");
        }
    };

    private final static String SMALL_CELL_CSS = Resources.INSTANCE.zicoCssResources().traceSmallCell();

    private AbstractCell<TraceRecordInfo> METHOD_PCT_CELL = new AbstractCell<TraceRecordInfo>() {
        @Override
        public void render(Context context, TraceRecordInfo rec, SafeHtmlBuilder sb) {
            double pct = 100.0 * rec.getTime() / trace.getExecutionTime();
            String color = "rgb(" + ((int) (pct * 2.49)) + ",0,0)";
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\" style=\"color: " + color + ";\">");
            sb.append(SafeHtmlUtils.fromString(NumberFormat.getFormat("###.0").format(pct) + "%"));
            sb.appendHtmlConstant("</div>");
        }
    };

    private void createResultsGrid() {

        resultsGrid = new DataGrid<TraceRecordInfo>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<TraceRecordInfo>(KEY_PROVIDER);
        resultsGrid.setSelectionModel(selectionModel);

        Column<TraceRecordInfo, TraceRecordInfo> colExpander
                = new IdentityColumn<TraceRecordInfo>(DETAIL_EXPANDER_CELL);
        resultsGrid.addColumn(colExpander, "#");
        resultsGrid.setColumnWidth(colExpander, 32, Style.Unit.PX);

        Column<TraceRecordInfo, TraceRecordInfo> colMethod = new IdentityColumn<TraceRecordInfo>(METHOD_CELL);
        resultsGrid.addColumn(colMethod, new ResizableHeader<TraceRecordInfo>("Method", resultsGrid, colMethod));
        resultsGrid.setColumnWidth(colMethod, 100, Style.Unit.PCT);

        Column<TraceRecordInfo, String> colTime = new Column<TraceRecordInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceRecordInfo rec) {
                return ClientUtil.formatDuration(rec.getTime());
            }
        };
        resultsGrid.addColumn(colTime, new ResizableHeader<TraceRecordInfo>("Time", resultsGrid, colTime));
        resultsGrid.setColumnWidth(colTime, 50, Style.Unit.PX);

        Column<TraceRecordInfo,String> colCalls = new Column<TraceRecordInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceRecordInfo rec) {
                return ""+rec.getCalls();
            }
        };
        resultsGrid.addColumn(colCalls, new ResizableHeader<TraceRecordInfo>("Calls", resultsGrid, colCalls));
        resultsGrid.setColumnWidth(colCalls, 50, Style.Unit.PX);

        Column<TraceRecordInfo,String> colErrors = new Column<TraceRecordInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceRecordInfo rec) {
                return ""+rec.getErrors();
            }
        };
        resultsGrid.addColumn(colErrors, new ResizableHeader<TraceRecordInfo>("Errors", resultsGrid, colErrors));
        resultsGrid.setColumnWidth(colErrors, 50, Style.Unit.PX);

        Column<TraceRecordInfo,TraceRecordInfo> colPct = new IdentityColumn<TraceRecordInfo>(METHOD_PCT_CELL);
        resultsGrid.addColumn(colPct, new ResizableHeader<TraceRecordInfo>("Pct", resultsGrid, colPct));
        resultsGrid.setColumnWidth(colPct, 50, Style.Unit.PX);

        rowBuilder = new TraceCallTableBuilder(resultsGrid, expandedDetails);
        resultsGrid.setTableBuilder(rowBuilder);

        resultsGrid.setSkipRowHoverStyleUpdate(true);
        resultsGrid.setSkipRowHoverFloatElementCheck(true);
        resultsGrid.setSkipRowHoverCheck(true);
        resultsGrid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        resultsGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceRecordInfo>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceRecordInfo> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    doGoTo();
                }
                //if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                //    selection.setSelected(event.getValue(), true);
                //    contextMenu.showAt(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
                //}
            }
        });

        resultsGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());
        resultsGrid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        resultsStore = new ListDataProvider<TraceRecordInfo>();
        resultsStore.addDataDisplay(resultsGrid);
    }


    private void toggleDetails(TraceRecordInfo rec) {
        String path = rec.getPath();
        if (expandedDetails.contains(path)) {
            expandedDetails.remove(path);
        } else {
            expandedDetails.add(path);
        }
        resultsGrid.redrawRow(resultsStore.getList().indexOf(rec));
    }


    private void doGoTo() {
        TraceRecordInfo tri = selectionModel.getSelectedObject();
        int idx = tri != null ? resultsStore.getList().indexOf(tri) : 0;
        panel.setResults(resultsStore.getList(), idx);
        window.hide();
    }


    public void setRootPath(String rootPath) {
        if (this.rootPath != rootPath) {
            this.rootPath = rootPath;
            this.resultsStore.getList().clear();
            this.lblSumStats.setText("n/a");
        }
    }


    private void doSearch() {

        TraceRecordSearchQuery expr = new TraceRecordSearchQuery();

        expr.setType(chkEql.getValue() ? TraceRecordSearchQuery.EQL_QUERY : TraceRecordSearchQuery.TXT_QUERY);

        GWT.log("Search Type=" + expr.getType());

        expr.setFlags(
                (chkErrorsOnly.getValue() ? TraceRecordSearchQuery.ERRORS_ONLY : 0)
                        | (chkMethodsWithAttrs.getValue() ? TraceRecordSearchQuery.METHODS_WITH_ATTRS : 0)
                        | (chkClass.getValue() ? TraceRecordSearchQuery.SEARCH_CLASSES : 0)
                        | (chkMethod.getValue() ? TraceRecordSearchQuery.SEARCH_METHODS : 0)
                        | (chkAttribs.getValue() ? TraceRecordSearchQuery.SEARCH_ATTRS : 0)
                        | (chkExceptionText.getValue() ? TraceRecordSearchQuery.SEARCH_EX_MSG : 0)
                        | (chkIgnoreCase.getValue() ? TraceRecordSearchQuery.IGNORE_CASE : 0));

        expr.setSearchExpr(txtSearchFilter.getText().length() > 0 ? txtSearchFilter.getText() : null);

        GWT.log("Search flags=" + expr.getFlags());

        md.info(MDS, "Searching records ...");

        expr.setHostName(trace.getHostName());
        expr.setTraceOffs(trace.getDataOffs());
        expr.setMinTime(0);
        expr.setPath(rootPath);

        traceDataService.searchRecords(expr, new MethodCallback<TraceRecordSearchResult>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error performing search request", e);
            }

            @Override
            public void onSuccess(Method method, TraceRecordSearchResult response) {
                resultsStore.getList().clear();
                resultsStore.getList().addAll(response.getResult());
                lblSumStats.setText(response.getResult().size() + " methods, "
                                + NumberFormat.getFormat("###.0").format(response.getRecurPct()) + "% of trace execution time. "
                                + "Time: " + ClientUtil.formatDuration(response.getRecurTime()) + " non-recursive"
                                + ", " + ClientUtil.formatDuration(response.getMinTime()) + " min, "
                                + ", " + ClientUtil.formatDuration(response.getMaxTime()) + " max."

                );
                txtSearchFilter.setFocus(true);
                md.clear(MDS);
            }
        });
    }

    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }

}