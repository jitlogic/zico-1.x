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
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
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
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.widgets.ResizableHeader;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.resources.ZicoDataGridResources;
import com.jitlogic.zico.client.widgets.MenuItem;
import com.jitlogic.zico.client.widgets.PopupMenu;
import com.jitlogic.zico.client.widgets.ToolButton;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.jitlogic.zico.shared.data.TraceRecordProxy;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TraceCallTreePanel extends Composite {
    interface TraceCallTreePanelUiBinder extends UiBinder<Widget, TraceCallTreePanel> { }
    private static TraceCallTreePanelUiBinder ourUiBinder = GWT.create(TraceCallTreePanelUiBinder.class);

    @UiField
    DockLayoutPanel panel;

    @UiField(provided = true)
    Resources resources;

    @UiField
    ToolButton btnParentMethod;

    @UiField
    ToolButton btnSlowestMethod;

    @UiField
    ToolButton btnErrorMethod;

    @UiField
    ToolButton btnExpandAll;

    @UiField
    ToolButton btnSearch;

    @UiField
    ToolButton btnSearchPrev;

    @UiField
    ToolButton btnSearchNext;

    @UiField(provided = true)
    DataGrid<TraceRecordProxy> grid;

    private static final ProvidesKey<TraceRecordProxy> KEY_PROVIDER = new ProvidesKey<TraceRecordProxy>() {
        @Override
        public Object getKey(TraceRecordProxy item) {
            return item.getPath();
        }
    };

    private ZicoRequestFactory rf;
    private TraceInfoProxy trace;

    private TraceRecordSearchDialog searchDialog;
    private PanelFactory panelFactory;

    private SingleSelectionModel<TraceRecordProxy> selection;
    private ListDataProvider<TraceRecordProxy> data;
    private TraceCallTableBuilder rowBuilder;

    private Set<String> expandedDetails = new HashSet<String>();

    private PopupMenu contextMenu;

    private boolean fullyExpanded;

    private List<TraceRecordProxy> searchResults = new ArrayList<TraceRecordProxy>();
    private int curentSearchResultIdx = -1;

    private MessageDisplay md;
    private final String MDS;

    @Inject
    public TraceCallTreePanel(ZicoRequestFactory rf, PanelFactory panelFactory,
                              MessageDisplay md, @Assisted TraceInfoProxy trace) {
        this.rf = rf;
        this.md = md;
        this.panelFactory = panelFactory;
        this.trace = trace;

        this.MDS = "TraceCallTree:" + trace.getHostName() + ":" + trace.getDataOffs();

        this.resources = Resources.INSTANCE;

        createCallTreeGrid();
        ourUiBinder.createAndBindUi(this);

        initWidget(panel);

        btnSearchPrev.setEnabled(false);
        btnSearchNext.setEnabled(false);

        createContextMenu();

        loadData(false, null);
    }


    private final static String SMALL_CELL_CSS = Resources.INSTANCE.zicoCssResources().traceSmallCell();

    private void createCallTreeGrid() {
        grid = new DataGrid<TraceRecordProxy>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selection = new SingleSelectionModel<TraceRecordProxy>(KEY_PROVIDER);
        grid.setSelectionModel(selection);

        Column<TraceRecordProxy, TraceRecordProxy> colExpander
                = new IdentityColumn<TraceRecordProxy>(DETAIL_EXPANDER_CELL);
        grid.addColumn(colExpander, "#");
        grid.setColumnWidth(colExpander, 32, Style.Unit.PX);

        Column<TraceRecordProxy, TraceRecordProxy> colMethodName
                = new IdentityColumn<TraceRecordProxy>(METHOD_TREE_CELL);
        grid.addColumn(colMethodName, new ResizableHeader<TraceRecordProxy>("Method", grid, colMethodName));
        grid.setColumnWidth(colMethodName, 100, Style.Unit.PCT);


        Column<TraceRecordProxy, TraceRecordProxy> colTime =
            new IdentityColumn<TraceRecordProxy>(METHOD_TIME_CELL);
        grid.addColumn(colTime, new ResizableHeader<TraceRecordProxy>("Time", grid, colTime));
        grid.setColumnWidth(colTime, 60, Style.Unit.PX);


        Column<TraceRecordProxy, TraceRecordProxy> colCalls =
            new IdentityColumn<TraceRecordProxy>(METHOD_CALLS_CELL);
        grid.addColumn(colCalls, new ResizableHeader<TraceRecordProxy>("Calls", grid, colCalls));
        grid.setColumnWidth(colCalls, 60, Style.Unit.PX);


        Column<TraceRecordProxy, TraceRecordProxy> colErrors =
            new IdentityColumn<TraceRecordProxy>(METHOD_ERRORS_CELL);
        grid.addColumn(colErrors, new ResizableHeader<TraceRecordProxy>("Errors", grid, colErrors));
        grid.setColumnWidth(colErrors, 60, Style.Unit.PX);


        Column<TraceRecordProxy, TraceRecordProxy> colPct =
            new IdentityColumn<TraceRecordProxy>(METHOD_PCT_CELL);
        grid.addColumn(colPct, new ResizableHeader<TraceRecordProxy>("Pct", grid, colPct));
        grid.setColumnWidth(colPct, 60, Style.Unit.PX);

        rowBuilder = new TraceCallTableBuilder(grid, expandedDetails);
        grid.setTableBuilder(rowBuilder);

        // Disable hovering "features" overall to improve performance on big trees.
        grid.setSkipRowHoverStyleUpdate(true);
        grid.setSkipRowHoverFloatElementCheck(true);
        grid.setSkipRowHoverCheck(true);
        grid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        grid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceRecordProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceRecordProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    TraceRecordProxy tr = event.getValue();
                    panelFactory.methodAttrsDialog(trace.getHostName(), trace.getDataOffs(), tr.getPath(), 0L).asPopupWindow().show();
                }
                if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                    selection.setSelected(event.getValue(), true);
                    contextMenu.setPopupPosition(
                            event.getNativeEvent().getClientX(),
                            event.getNativeEvent().getClientY());
                    contextMenu.show();
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

        data = new ListDataProvider<TraceRecordProxy>();
        data.addDataDisplay(grid);
    }


    private void createContextMenu() {
        contextMenu = new PopupMenu();


        MenuItem mnuMethodAttrs = new MenuItem("Trace Attributes", Resources.INSTANCE.methodAttrsIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        TraceRecordProxy tr = selection.getSelectedObject();
                        if (tr != null) {
                            panelFactory.methodAttrsDialog(trace.getHostName(), trace.getDataOffs(), tr.getPath(), 0L).asPopupWindow().show();
                        }
                    }
                });
        contextMenu.addItem(mnuMethodAttrs);
    }


    @UiHandler("btnSearch")
    void doSearch(ClickEvent e) {
        if (searchDialog == null) {
            searchDialog = panelFactory.traceRecordSearchDialog(this, trace);
        }
        searchDialog.asPopupWindow().show();
    }


    public void setResults(List<TraceRecordProxy> results, int idx) {
        this.searchResults = results;
        goToResult(idx);
    }

    private void goToResult(final int idx) {

        // Tree has to be fully expanded in order to search results
        if (!fullyExpanded) {
            this.loadData(true, new Runnable() {
                @Override
                public void run() {
                    goToResult(idx);
                }
            });
            return;
        }

        String path = searchResults.get(idx).getPath();

        if (path.length() == 0) {
            path = "/";
        }

        List<TraceRecordProxy> lst = data.getList();

        for (int i = 0; i < lst.size(); i++) {
            TraceRecordProxy tr = lst.get(i);
            String trPath = "/"+tr.getPath();
            if (trPath.equals(path)) {
                selection.setSelected(tr, true);
                grid.getRowElement(i).scrollIntoView();
                break;
            }
        }

        curentSearchResultIdx = idx;
        btnSearchPrev.setEnabled(idx > 0);
        btnSearchNext.setEnabled(idx < searchResults.size()-1);
    }


    private void loadData(final boolean recursive, final Runnable action) {

        if (recursive) {
            btnExpandAll.setEnabled(false);
        }
        data.getList().clear();
        if (recursive) {
            fullyExpanded = true;
        }

        md.info(MDS, "Loading data. Please wait ...");

        rf.traceDataService().listRecords(trace.getHostName(), trace.getDataOffs(), 0, null, recursive)
            .fire(new Receiver<List<TraceRecordProxy>>() {
                @Override
                public void onSuccess(List<TraceRecordProxy> response) {
                    data.setList(response);
                    if (action != null) {
                        action.run();
                    }
                    if (response.size() > 1) {
                        grid.getRowElement(0).scrollIntoView();
                    }
                    md.clear(MDS);
                }
                @Override
                public void onFailure(ServerFailure failure) {
                    md.error(MDS, "Error loading trace data", failure);
                }
            });
    }


    @UiHandler("btnParentMethod")
    void findParent(ClickEvent e) {
        TraceRecordProxy rec = selection.getSelectedObject();

        if (rec == null) {
            return;
        }

        List<TraceRecordProxy> recList = data.getList();

        int nsegs = rec.getPath().split("/").length;

        TraceRecordProxy prec = rec;
        int idx = 0;

        for (idx = recList.indexOf(rec)-1; idx >= 0; idx--) {
            prec = recList.get(idx);
            if (prec.getPath().split("/").length < nsegs) {
                break;
            }
        }

        selection.setSelected(prec, true);
        grid.getRowElement(idx >= 0 ? idx : 0).scrollIntoView();
    }


    @UiHandler("btnErrorMethod")
    void findErrorMethod(ClickEvent e) {

        if (!fullyExpanded) {
            this.loadData(true, new Runnable() {
                @Override
                public void run() {
                    findErrorMethod(null);
                }
            });
            return;
        }

        TraceRecordProxy rec = selection.getSelectedObject();
        List<TraceRecordProxy> recList = data.getList();
        if (rec == null) {
            rec = recList.get(0);
        }

        for (int i = recList.indexOf(rec)+1; i < recList.size(); i++) {
            TraceRecordProxy tr = recList.get(i);
            if (tr.getExceptionInfo() != null) {
                selection.setSelected(tr, true);
                grid.getRowElement(i).scrollIntoView();
                break;
            }
        }

    }


    @UiHandler("btnSlowestMethod")
    void findSlowestMethod(ClickEvent e) {
        TraceRecordProxy rec = selection.getSelectedObject();
        List<TraceRecordProxy> recList = data.getList();
        if (rec == null) {
            rec = recList.get(0);
        }

        TraceRecordProxy lrec = null;
        int lidx = -1, startIdx = recList.indexOf(rec);

        if (rec.getChildren() > 0 && !isExpanded(startIdx)) {
            doExpand(rec);
            return;
        }

        if (startIdx+1 < recList.size()) {
            for (int idx = startIdx+1; idx < recList.size(); idx++) {
                TraceRecordProxy r = recList.get(idx);
                if (r.getPath().startsWith(rec.getPath())) {
                    if (lrec == null || r.getTime() > lrec.getTime()) {
                        lrec = r;
                        lidx = idx;
                    }
                } else {
                    break;
                }
            }
        } else {
            return;
        }

        if (lrec != null) {
            selection.setSelected(lrec, true);
            grid.getRowElement(lidx).scrollIntoView();

            if (!isExpanded(lidx)) {
                doExpand(lrec);
            }
        }

    }


    @UiHandler("btnExpandAll")
    void expandAll(ClickEvent e) {
        loadData(true, null);
    }


    @UiHandler("btnSearchPrev")
    void goPrevResult(ClickEvent e) {
        goToResult(curentSearchResultIdx-1);
    }


    @UiHandler("btnSearchNext")
    void goNextResult(ClickEvent e) {
        goToResult(curentSearchResultIdx+1);
    }


    private void doExpand(final TraceRecordProxy rec) {
        md.info(MDS, "Loading trace data...");
        rf.traceDataService().listRecords(trace.getHostName(), trace.getDataOffs(), 0, rec.getPath(), false).fire(
                new Receiver<List<TraceRecordProxy>>() {
                    @Override
                    public void onSuccess(List<TraceRecordProxy> newrecs) {
                        List<TraceRecordProxy> list = data.getList();
                        int idx = list.indexOf(rec)+1;
                        for (int i = 0; i < newrecs.size(); i++) {
                            list.add(idx+i, newrecs.get(i));
                        }
                        md.clear(MDS);
                    }
                    @Override
                    public void onFailure(ServerFailure failure) {
                        md.error(MDS, "Error loading trace data", failure);
                    }
                }
        );
    }


    private void doCollapse(TraceRecordProxy rec) {
        List<TraceRecordProxy> list = data.getList();
        int idx = list.indexOf(rec) + 1;
        while (idx < list.size() && list.get(idx).getPath().startsWith(rec.getPath())) {
            list.remove(idx);
        }
    }


    private void toggleSubtree(TraceRecordProxy rec) {
        if (rec.getChildren() > 0) {
            if (isExpanded(rec)) {
                doCollapse(rec);
                btnExpandAll.setEnabled(true);
            } else {
                doExpand(rec);
            }
        }
        grid.redrawRow(data.getList().indexOf(rec));
    }


    private boolean isExpanded(TraceRecordProxy rec) {
        int idx = data.getList().indexOf(rec);
        return idx >= 0 ? isExpanded(idx) : false;
    }


    private boolean isExpanded(int idx) {
        List<TraceRecordProxy> lst = data.getList();
        if (idx < lst.size()-1) {
            TraceRecordProxy tr = lst.get(idx), ntr = lst.get(idx+1);
            return tr != null && ntr != null && ntr.getPath().startsWith(tr.getPath());
        } else {
            return false;
        }
    }


    private void toggleDetails(TraceRecordProxy rec) {
        String path = rec.getPath();
        if (expandedDetails.contains(path)) {
            expandedDetails.remove(path);
        } else {
            expandedDetails.add(path);
        }
        grid.redrawRow(data.getList().indexOf(rec));
    }


    private static final String PLUS_HTML = AbstractImagePrototype.create(Resources.INSTANCE.treePlusSlimIcon()).getHTML();
    private static final String MINUS_HTML = AbstractImagePrototype.create(Resources.INSTANCE.treeMinusSlimIcon()).getHTML();

    private static final String EXPANDER_EXPAND = AbstractImagePrototype.create(Resources.INSTANCE.expanderExpand()).getHTML();
    private static final String EXPANDER_COLLAPSE = AbstractImagePrototype.create(Resources.INSTANCE.expanderCollapse()).getHTML();


    private final Cell<TraceRecordProxy> METHOD_TREE_CELL = new AbstractCell<TraceRecordProxy>("click") {

        @Override
        public void render(Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {
            String path = tr.getPath();
            int offs = path != null && path.length() > 0 ? (path.split("/").length) * 24 : 0;
            String color = tr.getExceptionInfo() != null ? "red" : tr.getAttributes() != null ? "blue" : "black";

            sb.appendHtmlConstant("<div style=\"margin-left: " + offs + "px; color: " + color + ";margin-top: 3px;\">");
            if (tr.getChildren() > 0) {
                sb.appendHtmlConstant("<span style=\"cursor: pointer;\">");
                sb.appendHtmlConstant(isExpanded(context.getIndex()) ? MINUS_HTML : PLUS_HTML);
                sb.appendHtmlConstant("</span>");
            }
            sb.appendHtmlConstant("<span style=\"vertical-align: top;\">");
            sb.append(SafeHtmlUtils.fromString(tr.getMethod()));
            sb.appendHtmlConstant("</span></div>");
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, TraceRecordProxy rec,
                                   NativeEvent event, ValueUpdater<TraceRecordProxy> valueUpdater) {
            super.onBrowserEvent(context, parent, rec, event, valueUpdater);
            EventTarget eventTarget = event.getEventTarget();
            if (Element.is(eventTarget)) {
                Element target = eventTarget.cast();
                if ("IMG".equalsIgnoreCase(target.getTagName())) {
                    toggleSubtree(rec);
                }
            }
        }
    };

    private AbstractCell<TraceRecordProxy> METHOD_TIME_CELL = new AbstractCell<TraceRecordProxy>() {
        @Override
        public void render(Context context, TraceRecordProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getTime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceRecordProxy> METHOD_CALLS_CELL = new AbstractCell<TraceRecordProxy>() {
        @Override
        public void render(Context context, TraceRecordProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getCalls()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceRecordProxy> METHOD_ERRORS_CELL = new AbstractCell<TraceRecordProxy>() {
        @Override
        public void render(Context context, TraceRecordProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getErrors()));
            sb.appendHtmlConstant("</div>");
        }
    };

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

}
