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

import com.jitlogic.zico.widgets.client.*;
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
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.api.TraceDataService;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.shared.data.TraceInfo;
import com.jitlogic.zico.shared.data.TraceRecordInfo;
import com.jitlogic.zico.shared.data.TraceRecordListQuery;
import com.jitlogic.zico.widgets.client.MenuItem;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

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
    DataGrid<TraceRecordInfo> grid;

    private static final ProvidesKey<TraceRecordInfo> KEY_PROVIDER = new ProvidesKey<TraceRecordInfo>() {
        @Override
        public Object getKey(TraceRecordInfo item) {
            return item.getPath();
        }
    };

    private TraceInfo trace;

    private TraceDataService traceDataService;

    private TraceRecordSearchDialog searchDialog;
    private PanelFactory panelFactory;

    private SingleSelectionModel<TraceRecordInfo> selection;
    private ListDataProvider<TraceRecordInfo> data;
    private TraceCallTableBuilder rowBuilder;

    private Set<String> expandedDetails = new HashSet<String>();

    private PopupMenu contextMenu;

    private boolean fullyExpanded;

    private List<TraceRecordInfo> searchResults = new ArrayList<TraceRecordInfo>();
    private int curentSearchResultIdx = -1;

    private MessageDisplay md;
    private final String MDS;

    @Inject
    public TraceCallTreePanel(TraceDataService traceDataService, PanelFactory panelFactory,
                              MessageDisplay md, @Assisted TraceInfo trace) {
        this.md = md;
        this.traceDataService = traceDataService;
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

    private final static String SMALL_CELL_CSS_R = Resources.INSTANCE.zicoCssResources().traceSmallCellR();

    private void createCallTreeGrid() {
        grid = new DataGrid<TraceRecordInfo>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selection = new SingleSelectionModel<TraceRecordInfo>(KEY_PROVIDER);
        grid.setSelectionModel(selection);

        Column<TraceRecordInfo, TraceRecordInfo> colExpander
                = new IdentityColumn<TraceRecordInfo>(DETAIL_EXPANDER_CELL);
        grid.addColumn(colExpander, "#");
        grid.setColumnWidth(colExpander, 32, Style.Unit.PX);

        Column<TraceRecordInfo, TraceRecordInfo> colMethodName
                = new IdentityColumn<TraceRecordInfo>(METHOD_TREE_CELL);
        grid.addColumn(colMethodName, new ResizableHeader<TraceRecordInfo>("Method", grid, colMethodName));
        grid.setColumnWidth(colMethodName, 100, Style.Unit.PCT);


        Column<TraceRecordInfo, TraceRecordInfo> colTime =
            new IdentityColumn<TraceRecordInfo>(METHOD_TIME_CELL);
        grid.addColumn(colTime, new ResizableHeader<TraceRecordInfo>("Time", grid, colTime));
        grid.setColumnWidth(colTime, 60, Style.Unit.PX);
        colTime.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

        Column<TraceRecordInfo, TraceRecordInfo> colCTime =
                new IdentityColumn<TraceRecordInfo>(METHOD_CTIME_CELL);
        grid.addColumn(colCTime, new ResizableHeader<TraceRecordInfo>("CTime", grid, colTime));
        grid.setColumnWidth(colCTime, 60, Style.Unit.PX);
        colCTime.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

        Column<TraceRecordInfo, TraceRecordInfo> colCalls =
            new IdentityColumn<TraceRecordInfo>(METHOD_CALLS_CELL);
        grid.addColumn(colCalls, new ResizableHeader<TraceRecordInfo>("Calls", grid, colCalls));
        grid.setColumnWidth(colCalls, 60, Style.Unit.PX);


        Column<TraceRecordInfo, TraceRecordInfo> colErrors =
            new IdentityColumn<TraceRecordInfo>(METHOD_ERRORS_CELL);
        grid.addColumn(colErrors, new ResizableHeader<TraceRecordInfo>("Errors", grid, colErrors));
        grid.setColumnWidth(colErrors, 60, Style.Unit.PX);


        Column<TraceRecordInfo, TraceRecordInfo> colPct =
            new IdentityColumn<TraceRecordInfo>(METHOD_PCT_CELL);
        grid.addColumn(colPct, new ResizableHeader<TraceRecordInfo>("Pct", grid, colPct));
        grid.setColumnWidth(colPct, 60, Style.Unit.PX);

        rowBuilder = new TraceCallTableBuilder(grid, expandedDetails);
        grid.setTableBuilder(rowBuilder);

        // Disable hovering "features" overall to improve performance on big trees.
        grid.setSkipRowHoverStyleUpdate(true);
        grid.setSkipRowHoverFloatElementCheck(true);
        grid.setSkipRowHoverCheck(true);
        grid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        grid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceRecordInfo>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceRecordInfo> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    TraceRecordInfo tr = event.getValue();
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

        data = new ListDataProvider<TraceRecordInfo>();
        data.addDataDisplay(grid);
    }


    private void createContextMenu() {
        contextMenu = new PopupMenu();


        MenuItem mnuMethodAttrs = new MenuItem("Trace Attributes", Resources.INSTANCE.methodAttrsIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        TraceRecordInfo tr = selection.getSelectedObject();
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


    public void setResults(List<TraceRecordInfo> results, int idx) {
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

        List<TraceRecordInfo> lst = data.getList();

        for (int i = 0; i < lst.size(); i++) {
            TraceRecordInfo tr = lst.get(i);
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

        TraceRecordListQuery q = new TraceRecordListQuery();
        q.setHostName(trace.getHostName());
        q.setTraceOffs(trace.getDataOffs());
        q.setMinTime(0);
        q.setPath(null);
        q.setRecursive(recursive);

        traceDataService.listRecords(q, new MethodCallback<List<TraceRecordInfo>>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error loading trace data", e);
            }

            @Override
            public void onSuccess(Method method, List<TraceRecordInfo> response) {
                data.setList(response);
                if (action != null) {
                    action.run();
                }
                if (response.size() > 1) {
                    grid.getRowElement(0).scrollIntoView();
                }
                md.clear(MDS);
            }
        });
    }


    @UiHandler("btnParentMethod")
    void findParent(ClickEvent e) {
        TraceRecordInfo rec = selection.getSelectedObject();

        if (rec == null) {
            return;
        }

        List<TraceRecordInfo> recList = data.getList();

        int nsegs = rec.getPath().split("/").length;

        TraceRecordInfo prec = rec;
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

        TraceRecordInfo rec = selection.getSelectedObject();
        List<TraceRecordInfo> recList = data.getList();
        if (rec == null) {
            rec = recList.get(0);
        }

        for (int i = recList.indexOf(rec)+1; i < recList.size(); i++) {
            TraceRecordInfo tr = recList.get(i);
            if (tr.getExceptionInfo() != null) {
                selection.setSelected(tr, true);
                grid.getRowElement(i).scrollIntoView();
                break;
            }
        }

    }


    @UiHandler("btnSlowestMethod")
    void findSlowestMethod(ClickEvent e) {
        TraceRecordInfo rec = selection.getSelectedObject();
        List<TraceRecordInfo> recList = data.getList();
        if (rec == null) {
            rec = recList.get(0);
        }

        TraceRecordInfo lrec = null;
        int lidx = -1, startIdx = recList.indexOf(rec);

        if (rec.getChildren() > 0 && !isExpanded(startIdx)) {
            doExpand(rec);
            return;
        }

        if (startIdx+1 < recList.size()) {
            for (int idx = startIdx+1; idx < recList.size(); idx++) {
                TraceRecordInfo r = recList.get(idx);
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


    private void doExpand(final TraceRecordInfo rec) {
        md.info(MDS, "Loading trace data...");
        TraceRecordListQuery q = new TraceRecordListQuery();
        q.setHostName(trace.getHostName());;
        q.setTraceOffs(trace.getDataOffs());
        q.setMinTime(0);
        q.setPath(rec.getPath());
        q.setRecursive(false);

        traceDataService.listRecords(q, new MethodCallback<List<TraceRecordInfo>>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error loading trace data", e);
            }

            @Override
            public void onSuccess(Method method, List<TraceRecordInfo> records) {
                List<TraceRecordInfo> list = data.getList();
                int idx = list.indexOf(rec)+1;
                for (int i = 0; i < records.size(); i++) {
                    list.add(idx+i, records.get(i));
                }
                md.clear(MDS);
            }
        });
    }


    private void doCollapse(TraceRecordInfo rec) {
        List<TraceRecordInfo> list = data.getList();
        int idx = list.indexOf(rec) + 1;
        while (idx < list.size() && list.get(idx).getPath().startsWith(rec.getPath())) {
            list.remove(idx);
        }
    }


    private void toggleSubtree(TraceRecordInfo rec) {
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


    private boolean isExpanded(TraceRecordInfo rec) {
        int idx = data.getList().indexOf(rec);
        return idx >= 0 ? isExpanded(idx) : false;
    }


    private boolean isExpanded(int idx) {
        List<TraceRecordInfo> lst = data.getList();
        if (idx < lst.size()-1) {
            TraceRecordInfo tr = lst.get(idx), ntr = lst.get(idx+1);
            return tr != null && ntr != null && ntr.getPath().startsWith(tr.getPath());
        } else {
            return false;
        }
    }


    private void toggleDetails(TraceRecordInfo rec) {
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


    private final Cell<TraceRecordInfo> METHOD_TREE_CELL = new AbstractCell<TraceRecordInfo>("click") {

        @Override
        public void render(Context context, TraceRecordInfo tr, SafeHtmlBuilder sb) {
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
        public void onBrowserEvent(Context context, Element parent, TraceRecordInfo rec,
                                   NativeEvent event, ValueUpdater<TraceRecordInfo> valueUpdater) {
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

    private AbstractCell<TraceRecordInfo> METHOD_TIME_CELL = new AbstractCell<TraceRecordInfo>() {
        @Override
        public void render(Context context, TraceRecordInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS_R + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getTime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceRecordInfo> METHOD_CTIME_CELL = new AbstractCell<TraceRecordInfo>() {
        @Override
        public void render(Context context, TraceRecordInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS_R + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getCtime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceRecordInfo> METHOD_CALLS_CELL = new AbstractCell<TraceRecordInfo>() {
        @Override
        public void render(Context context, TraceRecordInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS_R + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getCalls()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceRecordInfo> METHOD_ERRORS_CELL = new AbstractCell<TraceRecordInfo>() {
        @Override
        public void render(Context context, TraceRecordInfo rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS_R + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getErrors()));
            sb.appendHtmlConstant("</div>");
        }
    };

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

}
