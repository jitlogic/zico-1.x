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
package com.jitlogic.zico.client.views.hosts;

import com.google.gwt.cell.client.AbstractCell;
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
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.api.HostService;
import com.jitlogic.zico.client.views.Shell;
import com.jitlogic.zico.client.widgets.*;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.widgets.MenuItem;
import com.jitlogic.zico.shared.data.HostInfo;
import com.jitlogic.zico.shared.data.HostListObject;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class HostListPanel extends Composite {
    interface HostListPanelUiBinder extends UiBinder<Widget, HostListPanel> { }
    private static HostListPanelUiBinder ourUiBinder = GWT.create(HostListPanelUiBinder.class);

    @UiField
    DockLayoutPanel panel;

    @UiField
    HorizontalPanel toolbar;

    @UiField(provided = true)
    Resources resources;

    @UiField
    ToolButton btnRefresh;

    @UiField
    ToolButton btnAddHost;

    @UiField
    ToolButton btnRemoveHost;

    @UiField
    ToolButton btnEditHost;

    @UiField
    ToolButton btnDisableHost;

    @UiField
    ToolButton btnEnableHost;

    @UiField
    ToolButton btnListTraces;

    @UiField(provided = true)
    DataGrid<HostListObject> hostGrid;

    private Provider<Shell> shell;
    private PanelFactory panelFactory;

    private HostService hostService;

    private ListDataProvider<HostListObject> hostGridStore;
    private SingleSelectionModel<HostListObject> selectionModel;

    private Map<String,HostGroup> hostGroups = new TreeMap<String,HostGroup>();

    private MenuItem mnuRefresh, mnuAddHost, mnuRemoveHost, mnuEditHost, mnuListTraces, mnuDisableHost, mnuEnableHost;

    private boolean selectionDependentControlsEnabled = true;

    private boolean adminMode = false;
    private PopupMenu contextMenu;

    private MessageDisplay md;

    private static final String MDS = "HostListPanel";

    @Inject
    public HostListPanel(Provider<Shell> shell, PanelFactory panelFactory,
                         HostService hostService, MessageDisplay md) {

        this.shell = shell;
        this.panelFactory = panelFactory;
        this.hostService = hostService;
        this.md = md;
        this.resources = Resources.INSTANCE;

        createHostGrid();

        ourUiBinder.createAndBindUi(this);

        createContextMenu();

        enableSelectionDependentControls(null);

        initWidget(panel);

        refresh(null);
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        enableSelectionDependentControls(null);
    }


    private void enableSelectionDependentControls(HostListObject hostInfo) {
        boolean enabled = hostInfo != null;
        boolean hostDisabled = hostInfo != null && !hostInfo.isEnabled();
        if (selectionDependentControlsEnabled != enabled) {
            btnRemoveHost.setEnabled(enabled && adminMode);
            btnEditHost.setEnabled(enabled && adminMode);
            btnListTraces.setEnabled(enabled);
            mnuRemoveHost.setEnabled(enabled && adminMode);
            mnuEditHost.setEnabled(enabled && adminMode);
            mnuListTraces.setEnabled(enabled);
            selectionDependentControlsEnabled = enabled;
        }

        btnDisableHost.setEnabled(enabled && !hostDisabled && adminMode);
        btnEnableHost.setEnabled(hostDisabled && adminMode);
        mnuDisableHost.setEnabled(enabled && !hostDisabled && adminMode);
        mnuEnableHost.setEnabled(hostDisabled && adminMode);

        btnAddHost.setEnabled(adminMode);
        mnuAddHost.setEnabled(adminMode);
    }

    private static final ProvidesKey<HostListObject> KEY_PROVIDER = new ProvidesKey<HostListObject>() {
        @Override
        public Object getKey(HostListObject item) {
            return item.getName();
        }
    };

    private static final String PLUS_HTML = AbstractImagePrototype.create(Resources.INSTANCE.treePlusSlimIcon()).getHTML();
    private static final String MINUS_HTML = AbstractImagePrototype.create(Resources.INSTANCE.treeMinusSlimIcon()).getHTML();

    private final Cell<HostListObject> EXPAND_CELL = new AbstractCell<HostListObject>("click") {
        @Override
        public void render(Context context, HostListObject v, SafeHtmlBuilder sb) {
            if (v instanceof HostGroup) {
                HostGroup hg = (HostGroup)v;
                sb.appendHtmlConstant("<span style=\"cursor: pointer;\">");
                sb.appendHtmlConstant(hg.isExpanded() ? MINUS_HTML : PLUS_HTML);
                sb.appendHtmlConstant("</span>");
            } else {
                sb.appendHtmlConstant("<div/>");
            }
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, HostListObject v,
                                   NativeEvent event, ValueUpdater<HostListObject> valueUpdater) {
            super.onBrowserEvent(context, parent, v, event, valueUpdater);
            EventTarget eventTarget = event.getEventTarget();
            if (v instanceof HostGroup && Element.is(eventTarget)) {
                Element target = eventTarget.cast();
                if ("IMG".equalsIgnoreCase(target.getTagName())) {
                    ((HostGroup)v).toggleExpanded();
                    redrawHostList();
                }
            }
        }
    };

    private static final Cell<HostListObject> NAME_CELL = new AbstractCell<HostListObject>() {
        @Override
        public void render(Context context, HostListObject host, SafeHtmlBuilder sb) {
            if (host instanceof HostInfo) {
                String color = (host.isEnabled()) ? "black" : "gray";
                sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
                sb.append(SafeHtmlUtils.fromString(host.getName()));
                sb.appendHtmlConstant("</span>");
            } else {
                sb.appendHtmlConstant("<span style=\"font-weight: bold;\">");
                sb.append(SafeHtmlUtils.fromString(host.getName()));
                sb.appendHtmlConstant("</span>");
            }
        }
    };

    private static final Cell<HostListObject> ADDRESS_CELL = new AbstractCell<HostListObject>() {
        @Override
        public void render(Context context, HostListObject host, SafeHtmlBuilder sb) {
            String color = (host.isEnabled()) ? "black" : "gray";
            sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
            sb.append(SafeHtmlUtils.fromString(host.getAddr()));
            sb.appendHtmlConstant("</span>");
        }
    };

    private void createHostGrid() {

        hostGrid = new DataGrid<HostListObject>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<HostListObject>(KEY_PROVIDER);
        hostGrid.setSelectionModel(selectionModel);

        Column<HostListObject,HostListObject> colExpand = new IdentityColumn<HostListObject>(EXPAND_CELL);
        hostGrid.addColumn(colExpand, new ResizableHeader<HostListObject>("#", hostGrid, colExpand));
        hostGrid.setColumnWidth(colExpand, 36, Style.Unit.PX);

        Column<HostListObject,HostListObject> colName = new IdentityColumn<HostListObject>(NAME_CELL);
        hostGrid.addColumn(colName, new ResizableHeader<HostListObject>("Name", hostGrid, colName));
        hostGrid.setColumnWidth(colName, 140, Style.Unit.PX);

        Column<HostListObject,HostListObject> colAddr = new IdentityColumn<HostListObject>(ADDRESS_CELL);
        hostGrid.addColumn(colAddr, "Address");
        hostGrid.setColumnWidth(colAddr, 60, Style.Unit.PCT);

        hostGrid.setSkipRowHoverStyleUpdate(true);
        hostGrid.setSkipRowHoverFloatElementCheck(true);
        hostGrid.setSkipRowHoverCheck(true);
        hostGrid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        hostGridStore = new ListDataProvider<HostListObject>();
        hostGridStore.addDataDisplay(hostGrid);

        hostGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<HostListObject>() {
            @Override
            public void onCellPreview(CellPreviewEvent<HostListObject> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selectionModel.setSelected(event.getValue(), true);

                    enableSelectionDependentControls(event.getValue());
                    listTraces(null);
                }
                if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                    selectionModel.setSelected(event.getValue(), true);
                    if (event.getValue() != null) {
                        contextMenu.setPopupPosition(
                                event.getNativeEvent().getClientX(),
                                event.getNativeEvent().getClientY());
                        contextMenu.show();
                    }
                }

                // TODO update toolbar icons and context menu on element selection
            }
        });

        hostGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());

        hostGrid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                enableSelectionDependentControls(selectionModel.getSelectedObject());
            }
        });
    }


    private void rebuildHostGroups(List<HostInfo> hlist) {
        for (Map.Entry<String,HostGroup> e : hostGroups.entrySet()) {
            e.getValue().clear();
        }

        for (HostInfo host : hlist) {
            String groupName = host.getGroup().length() > 0 ? host.getGroup() : "(default)";
            if (!hostGroups.containsKey(groupName)) {
                hostGroups.put(groupName, new HostGroup(groupName));
            }
            hostGroups.get(groupName).addHost(host);
        }

        selectionModel.setSelected(selectionModel.getSelectedObject(), false);
        enableSelectionDependentControls(null);

    }

    private void redrawHostList() {
        List<HostListObject> hl = hostGridStore.getList();

        hl.clear();

        for (Map.Entry<String,HostGroup> e : hostGroups.entrySet()) {
            HostGroup hg = e.getValue();

            if (hg.size() > 0) {
                hl.add(hg);
                if (hg.isExpanded()) {
                    hl.addAll(hg.getHosts());
                }
            }
        }
    }



    private void createContextMenu() {
        contextMenu = new PopupMenu();


        mnuRefresh = new MenuItem("Refresh", Resources.INSTANCE.refreshIcon(), new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                refresh(null);
            }
        });
        contextMenu.addItem(mnuRefresh);

        contextMenu.addSeparator();

        mnuAddHost = new MenuItem("New host", Resources.INSTANCE.addIcon(), new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                addHost(null);
            }
        });
        contextMenu.addItem(mnuAddHost);

        mnuRemoveHost = new MenuItem("Remove Host", Resources.INSTANCE.removeIcon(), new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                removeHost(null);
            }
        });
        contextMenu.addItem(mnuRemoveHost);

        mnuEditHost = new MenuItem("Edit host", Resources.INSTANCE.editIcon(), new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                editHost(null);
            }
        });
        contextMenu.addItem(mnuEditHost);

        contextMenu.addSeparator();

        mnuDisableHost = new MenuItem("Disable Host", Resources.INSTANCE.disableIcon(), new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                toggleHost(false);
            }
        });
        contextMenu.addItem(mnuDisableHost);

        mnuEnableHost = new MenuItem("Enable Host", Resources.INSTANCE.enableIcon(), new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                toggleHost(true);
            }
        });
        contextMenu.addItem(mnuEnableHost);

        contextMenu.addSeparator();

        mnuListTraces = new MenuItem("List traces", Resources.INSTANCE.listColumnsIcon(), new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                listTraces(null);
            }
        });
        contextMenu.addItem(mnuListTraces);
    }



    private void toggleHost(boolean enabled) {
        HostListObject info = selectionModel.getSelectedObject();
        if (info instanceof HostInfo) {
            md.info(MDS, "Disabling host");
            ((HostInfo) info).setEnabled(enabled);
            hostService.update(info.getName(), (HostInfo)info, new MethodCallback<Void>() {
                @Override
                public void onFailure(Method method, Throwable e) {
                    md.error(MDS, "Error enabling/disabling host", e);
                }

                @Override
                public void onSuccess(Method method, Void response) {
                    refresh(null);
                    md.clear(MDS);
                }
            });
        }
    }


    @UiHandler("btnRefresh")
    void refresh(ClickEvent _) {
        hostGridStore.getList().clear();
        md.info(MDS, "Loading host list ...");
        hostService.list(new MethodCallback<List<HostInfo>>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error loading host list.", e);
            }

            @Override
            public void onSuccess(Method method, List<HostInfo> response) {
                md.clear(MDS);
                rebuildHostGroups(response);
                redrawHostList();
            }
        });
    }


    @UiHandler("btnAddHost")
    void addHost(ClickEvent e) {
        new HostEditDialog(hostService, this, null, md).getWindow().show();
    }


    @UiHandler("btnRemoveHost")
    void removeHost(ClickEvent e) {
        // TODO "Are you sure" message box
        final HostListObject hi = selectionModel.getSelectedObject();
        if (hi instanceof HostInfo) {

            ConfirmDialog dialog = new ConfirmDialog("Removing host", "Remove host " + hi.getName() + " ?")
                    .withBtn("Yes", new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            hostGridStore.getList().remove(hi);
                            hostService.delete(hi.getName(), new MethodCallback<Void>() {
                                @Override
                                public void onFailure(Method method, Throwable exception) {

                                }

                                @Override
                                public void onSuccess(Method method, Void response) {

                                }
                            });
                        }})
                    .withBtn("No");
            dialog.show();
        }
    }


    @UiHandler("btnEditHost")
    void editHost(ClickEvent e) {
        HostListObject hostInfo = selectionModel.getSelectedObject();
        if (hostInfo instanceof HostInfo) {
            new HostEditDialog(hostService, this, (HostInfo)hostInfo, md).getWindow().show();
        }
    }


    @UiHandler("btnDisableHost")
    void disableHost(ClickEvent e) {
        toggleHost(false);
    }


    @UiHandler("btnEnableHost")
    void enableHost(ClickEvent e) {
        toggleHost(true);
    }


    @UiHandler("btnListTraces")
    void listTraces(ClickEvent e) {
        HostListObject hostInfo = selectionModel.getSelectedObject();
        GWT.log("Selected host: " + hostInfo);

        if (hostInfo instanceof HostInfo && 0 == (((HostInfo)hostInfo).getFlags() & HostInfo.DISABLED)) {
            shell.get().addView(panelFactory.traceSearchPanel((HostInfo)hostInfo), hostInfo.getName() + ": traces");
        }
    }
}
