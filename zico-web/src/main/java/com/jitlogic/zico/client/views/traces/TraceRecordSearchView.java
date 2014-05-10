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
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.*;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.resources.ZicoDataGridResources;
import com.jitlogic.zico.client.widgets.IsPopupWindow;
import com.jitlogic.zico.client.widgets.PopupWindow;
import com.jitlogic.zico.client.widgets.ResizableHeader;
import com.jitlogic.zico.core.model.TraceRecordSearchQuery;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.jitlogic.zico.shared.data.TraceRecordProxy;
import com.jitlogic.zico.shared.data.TraceRecordSearchQueryProxy;
import com.jitlogic.zico.shared.data.TraceRecordSearchResultProxy;
import com.jitlogic.zico.shared.services.TraceDataServiceProxy;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class TraceRecordSearchView implements IsPopupWindow {
    interface TraceRecordSearchViewUiBinder extends UiBinder<Widget, TraceRecordSearchView> { }
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
    DataGrid<TraceRecordProxy> resultsGrid;

    private ListDataProvider<TraceRecordProxy> resultsStore;
    private TraceCallTableBuilder rowBuilder;
    private SingleSelectionModel<TraceRecordProxy> selectionModel;
    private Set<String> expandedDetails = new HashSet<String>();

    private TraceInfoProxy trace;
    private String rootPath = "";

    private TraceCallTreePanel panel;
    private ZicoRequestFactory rf;

    private com.jitlogic.zico.client.ErrorHandler errorHandler;

    private PopupWindow window;

    @Inject
    public TraceRecordSearchView(ZicoRequestFactory rf, ErrorHandler errorHandler,
                                 @Assisted TraceCallTreePanel panel, @Assisted TraceInfoProxy trace) {

        this.rf = rf;
        this.trace = trace;
        this.panel = panel;
        this.errorHandler = errorHandler;

        createResultsGrid();

        window = new PopupWindow(ourUiBinder.createAndBindUi(this));
        window.setCaption("Search for methods");
        window.resizeAndCenter(1200, 700);

        txtSearchFilter.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    doSearch();
                }
            }
        });
        chkIgnoreCase.setValue(true);
    }

    @UiHandler("btnSearch")
    void clickSearch(ClickEvent e) {
        doSearch();
    }

    @UiHandler("btnGoTo")
    void clickGoTo(ClickEvent e) {
        doSearch();
    }

    private static final ProvidesKey<TraceRecordProxy> KEY_PROVIDER = new ProvidesKey<TraceRecordProxy>() {
        @Override
        public Object getKey(TraceRecordProxy rec) {
            return rec.getPath();
        }
    };

    private static final String EXPANDER_EXPAND = AbstractImagePrototype.create(Resources.INSTANCE.expanderExpand()).getHTML();
    private static final String EXPANDER_COLLAPSE = AbstractImagePrototype.create(Resources.INSTANCE.expanderCollapse()).getHTML();

    private final Cell<TraceRecordProxy> DETAIL_EXPANDER_CELL = new ActionCell<TraceRecordProxy>("",
            new ActionCell.Delegate<TraceRecordProxy>() {
                @Override
                public void execute(TraceRecordProxy rec) {
                    toggleDetails(rec);
                }
            }) {
        @Override
        public void render(Cell.Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {
            if ((tr.getAttributes() != null && tr.getAttributes().size() > 0)||tr.getExceptionInfo() != null) {
                sb.appendHtmlConstant("<span style=\"cursor: pointer;\">");
                sb.appendHtmlConstant(expandedDetails.contains(tr.getPath()) ? EXPANDER_COLLAPSE : EXPANDER_EXPAND);
                sb.appendHtmlConstant("</span>");
            }
        }
    };

    private AbstractCell<TraceRecordProxy> METHOD_CELL = new AbstractCell<TraceRecordProxy>() {
        @Override
        public void render(Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {
            String color = tr.getExceptionInfo() != null ? "red"
                    : tr.getAttributes() != null ? "blue" : "black";
            sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
            sb.append(SafeHtmlUtils.fromString(tr.getMethod()));
            sb.appendHtmlConstant("</span>");
        }
    };

    private final static String SMALL_CELL_CSS = Resources.INSTANCE.zicoCssResources().traceSmallCell();

    private AbstractCell<TraceRecordProxy> METHOD_PCT_CELL = new AbstractCell<TraceRecordProxy>() {
        @Override
        public void render(Context context, TraceRecordProxy rec, SafeHtmlBuilder sb) {
            double pct = 100.0 * rec.getTime() / trace.getExecutionTime();
            String color = "rgb(" + ((int) (pct * 2.49)) + ",0,0)";
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\" style=\"color: " + color + ";\">");
            sb.append(SafeHtmlUtils.fromString(NumberFormat.getFormat("###.0").format(pct) + "%"));
            sb.appendHtmlConstant("</div>");
        }
    };

    private void createResultsGrid() {

        resultsGrid = new DataGrid<TraceRecordProxy>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<TraceRecordProxy>(KEY_PROVIDER);
        resultsGrid.setSelectionModel(selectionModel);

        Column<TraceRecordProxy, TraceRecordProxy> colExpander
                = new IdentityColumn<TraceRecordProxy>(DETAIL_EXPANDER_CELL);
        resultsGrid.addColumn(colExpander, "#");
        resultsGrid.setColumnWidth(colExpander, 32, Style.Unit.PX);

        Column<TraceRecordProxy, TraceRecordProxy> colMethod = new IdentityColumn<TraceRecordProxy>(METHOD_CELL);
        resultsGrid.addColumn(colMethod, new ResizableHeader<TraceRecordProxy>("Method", resultsGrid, colMethod));
        resultsGrid.setColumnWidth(colMethod, 100, Style.Unit.PCT);

        Column<TraceRecordProxy, String> colTime = new Column<TraceRecordProxy, String>(new TextCell()) {
            @Override
            public String getValue(TraceRecordProxy rec) {
                return ClientUtil.formatDuration(rec.getTime());
            }
        };
        resultsGrid.addColumn(colTime, new ResizableHeader<TraceRecordProxy>("Time", resultsGrid, colTime));
        resultsGrid.setColumnWidth(colTime, 50, Style.Unit.PX);

        Column<TraceRecordProxy,String> colCalls = new Column<TraceRecordProxy, String>(new TextCell()) {
            @Override
            public String getValue(TraceRecordProxy rec) {
                return ""+rec.getCalls();
            }
        };
        resultsGrid.addColumn(colCalls, new ResizableHeader<TraceRecordProxy>("Calls", resultsGrid, colCalls));
        resultsGrid.setColumnWidth(colCalls, 50, Style.Unit.PX);

        Column<TraceRecordProxy,String> colErrors = new Column<TraceRecordProxy, String>(new TextCell()) {
            @Override
            public String getValue(TraceRecordProxy rec) {
                return ""+rec.getErrors();
            }
        };
        resultsGrid.addColumn(colErrors, new ResizableHeader<TraceRecordProxy>("Errors", resultsGrid, colErrors));
        resultsGrid.setColumnWidth(colErrors, 50, Style.Unit.PX);

        Column<TraceRecordProxy,TraceRecordProxy> colPct = new IdentityColumn<TraceRecordProxy>(METHOD_PCT_CELL);
        resultsGrid.addColumn(colPct, new ResizableHeader<TraceRecordProxy>("Pct", resultsGrid, colPct));
        resultsGrid.setColumnWidth(colPct, 50, Style.Unit.PX);

        rowBuilder = new TraceCallTableBuilder(resultsGrid, expandedDetails);
        resultsGrid.setTableBuilder(rowBuilder);

        resultsGrid.setSkipRowHoverStyleUpdate(true);
        resultsGrid.setSkipRowHoverFloatElementCheck(true);
        resultsGrid.setSkipRowHoverCheck(true);
        resultsGrid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        resultsGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceRecordProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceRecordProxy> event) {
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

        resultsStore = new ListDataProvider<TraceRecordProxy>();
        resultsStore.addDataDisplay(resultsGrid);
    }


    private void toggleDetails(TraceRecordProxy rec) {
        String path = rec.getPath();
        if (expandedDetails.contains(path)) {
            expandedDetails.remove(path);
        } else {
            expandedDetails.add(path);
        }
        resultsGrid.redrawRow(resultsStore.getList().indexOf(rec));
    }


    private void doGoTo() {
        TraceRecordProxy tri = selectionModel.getSelectedObject();
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
        TraceDataServiceProxy req = rf.traceDataService();
        TraceRecordSearchQueryProxy expr = req.create(TraceRecordSearchQueryProxy.class);

        expr.setType(chkEql.getValue() ? TraceRecordSearchQuery.EQL_QUERY : TraceRecordSearchQuery.TXT_QUERY);

        expr.setFlags(
                (chkErrorsOnly.getValue() ? TraceRecordSearchQuery.ERRORS_ONLY : 0)
                        | (chkMethodsWithAttrs.getValue() ? TraceRecordSearchQuery.METHODS_WITH_ATTRS : 0)
                        | (chkClass.getValue() ? TraceRecordSearchQuery.SEARCH_CLASSES : 0)
                        | (chkMethod.getValue() ? TraceRecordSearchQuery.SEARCH_METHODS : 0)
                        | (chkAttribs.getValue() ? TraceRecordSearchQuery.SEARCH_ATTRS : 0)
                        | (chkExceptionText.getValue() ? TraceRecordSearchQuery.SEARCH_EX_MSG : 0)
                        | (chkIgnoreCase.getValue() ? TraceRecordSearchQuery.IGNORE_CASE : 0));

        expr.setSearchExpr(txtSearchFilter.getText());

        req.searchRecords(trace.getHostName(), trace.getDataOffs(), 0, rootPath, expr).fire(
                new Receiver<TraceRecordSearchResultProxy>() {
                    @Override
                    public void onSuccess(TraceRecordSearchResultProxy response) {
                        resultsStore.getList().clear();
                        resultsStore.getList().addAll(response.getResult());
                        lblSumStats.setText(response.getResult().size() + " methods, "
                                        + NumberFormat.getFormat("###.0").format(response.getRecurPct()) + "% of trace execution time. "
                                        + "Time: " + ClientUtil.formatDuration(response.getRecurTime()) + " non-recursive"
                                        + ", " + ClientUtil.formatDuration(response.getMinTime()) + " min, "
                                        + ", " + ClientUtil.formatDuration(response.getMaxTime()) + " max."

                        );
                        txtSearchFilter.setFocus(true);
                    }
                    @Override
                    public void onFailure(ServerFailure failure) {
                        errorHandler.error("Error performing search request", failure);
                    }
                }
        );
    }

    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }

}