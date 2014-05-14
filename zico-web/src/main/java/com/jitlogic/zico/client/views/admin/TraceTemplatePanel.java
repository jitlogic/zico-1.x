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
package com.jitlogic.zico.client.views.admin;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.widgets.ResizableHeader;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.widgets.MenuItem;
import com.jitlogic.zico.client.widgets.PopupMenu;
import com.jitlogic.zico.client.widgets.ToolButton;
import com.jitlogic.zico.shared.data.TraceTemplateProxy;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class TraceTemplatePanel extends Composite {

    private ListDataProvider<TraceTemplateProxy> templateStore;
    private DataGrid<TraceTemplateProxy> templateGrid;
    private SingleSelectionModel<TraceTemplateProxy> selectionModel;

    private ZicoRequestFactory rf;

    private PopupMenu contextMenu;

    private DockLayoutPanel panel;

    private MessageDisplay messageDisplay;

    @Inject
    public TraceTemplatePanel(ZicoRequestFactory rf, MessageDisplay messageDisplay) {

        this.messageDisplay = messageDisplay;
        this.rf = rf;

        panel = new DockLayoutPanel(Style.Unit.PX);
        initWidget(panel);

        createToolbar();
        createContextMenu();
        createTemplateListGrid();

        refreshTemplates();
    }

    private final static ProvidesKey<TraceTemplateProxy> KEY_PROVIDER = new ProvidesKey<TraceTemplateProxy>() {
        @Override
        public Object getKey(TraceTemplateProxy item) {
            return item.getId();
        }
    };

    private final static Cell<TraceTemplateProxy> ORDER_CELL = new AbstractCell<TraceTemplateProxy>() {
        @Override
        public void render(Context context, TraceTemplateProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(""+value.getOrder()));
        }
    };

    private final static Cell<TraceTemplateProxy> CONDITION_CELL = new AbstractCell<TraceTemplateProxy>() {
        @Override
        public void render(Context context, TraceTemplateProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(""+value.getCondition()));
        }
    };

    private final static Cell<TraceTemplateProxy> TEMPLATE_CELL = new AbstractCell<TraceTemplateProxy>() {
        @Override
        public void render(Context context, TraceTemplateProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(""+value.getTemplate()));
        }
    };


    private void createTemplateListGrid() {
        templateGrid = new DataGrid<TraceTemplateProxy>(1024*1024, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<TraceTemplateProxy>(KEY_PROVIDER);
        templateGrid.setSelectionModel(selectionModel);

        Column<TraceTemplateProxy,TraceTemplateProxy> colOrder = new IdentityColumn<TraceTemplateProxy>(ORDER_CELL);
        templateGrid.addColumn(colOrder, new ResizableHeader<TraceTemplateProxy>("Order", templateGrid, colOrder));
        templateGrid.setColumnWidth(colOrder, 80, Style.Unit.PX);

        Column<TraceTemplateProxy,TraceTemplateProxy> colCondition = new IdentityColumn<TraceTemplateProxy>(CONDITION_CELL);
        templateGrid.addColumn(colCondition, new ResizableHeader<TraceTemplateProxy>("Condition", templateGrid, colCondition));
        templateGrid.setColumnWidth(colCondition, 250, Style.Unit.PX);

        Column<TraceTemplateProxy,TraceTemplateProxy> colTemplate = new IdentityColumn<TraceTemplateProxy>(TEMPLATE_CELL);
        templateGrid.addColumn(colTemplate, "Description Template");
        templateGrid.setColumnWidth(colTemplate, 100, Style.Unit.PCT);

        templateStore = new ListDataProvider<TraceTemplateProxy>(KEY_PROVIDER);
        templateStore.addDataDisplay(templateGrid);

        templateGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceTemplateProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceTemplateProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selectionModel.setSelected(event.getValue(), true);
                    editTemplate();
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

            }
        });

        templateGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());

        templateGrid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        panel.add(templateGrid);
    }


    private void createToolbar() {
        HorizontalPanel toolBar = new HorizontalPanel();

        ToolButton btnRefresh = new ToolButton(Resources.INSTANCE.refreshIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        refreshTemplates();
                    }
                });
        //btnRefresh.setToolTip("Refresh list");

        toolBar.add(btnRefresh);

        //toolBar.add(new SeparatorToolItem());

        ToolButton btnNew = new ToolButton(Resources.INSTANCE.addIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        addTemplate();
                    }
                });
        //btnNew.setToolTip("Add new template");

        toolBar.add(btnNew);

        ToolButton btnRemove = new ToolButton(Resources.INSTANCE.removeIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        removeTemplate();
                    }
                });
        //btnRemove.setToolTip("Remove template");

        toolBar.add(btnRemove);

        //toolBar.add(new SeparatorToolItem());

        ToolButton btnEdit = new ToolButton(Resources.INSTANCE.editIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        editTemplate();
                    }
                });
        //btnEdit.setToolTip("Modify template");

        toolBar.add(btnEdit);

        panel.addNorth(toolBar, 32);
    }


    private void createContextMenu() {
        contextMenu = new PopupMenu();

        MenuItem mnuRefresh = new MenuItem("Refresh", Resources.INSTANCE.refreshIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        refreshTemplates();
                    }
                });
        contextMenu.addItem(mnuRefresh);

        contextMenu.addSeparator();

        MenuItem mnuCreateTemplate = new MenuItem("New template", Resources.INSTANCE.addIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        addTemplate();
                    }
                });
        contextMenu.addItem(mnuCreateTemplate);

        MenuItem mnuRemoveTemplate = new MenuItem("Remove template", Resources.INSTANCE.removeIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        removeTemplate();
                    }
                });
        contextMenu.addItem(mnuRemoveTemplate);

        contextMenu.addSeparator();

        MenuItem mnuEditTemplate = new MenuItem("Edit Template", Resources.INSTANCE.editIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        editTemplate();
                    }
                });
        contextMenu.addItem(mnuEditTemplate);
    }


    private void editTemplate() {
        TraceTemplateProxy tti = selectionModel.getSelectedObject();
        if (tti != null) {
            new TraceTemplateView(rf, this, tti, messageDisplay).asPopupWindow().show();
        }
    }


    private void addTemplate() {
        new TraceTemplateView(rf, this, null, messageDisplay).asPopupWindow().show();
    }


    private void removeTemplate() {
        TraceTemplateProxy template = selectionModel.getSelectedObject();
        if (template != null) {
            rf.systemService().removeTemplate(template.getId()).fire(
                    new Receiver<Void>() {
                        @Override
                        public void onSuccess(Void response) {
                            refreshTemplates();
                        }
                    }
            );
        }
    }

    private final static String SRC = "TraceTemplatePanel";

    public void refreshTemplates() {
        messageDisplay.info(SRC, "Loading trace display templates");
        rf.systemService().listTemplates().fire(new Receiver<List<TraceTemplateProxy>>() {
            @Override
            public void onSuccess(List<TraceTemplateProxy> response) {
                Collections.sort(response, new Comparator<TraceTemplateProxy>() {
                    @Override
                    public int compare(TraceTemplateProxy o1, TraceTemplateProxy o2) {
                        return o1.getOrder() - o2.getOrder();
                    }
                });
                templateStore.getList().clear();
                templateStore.getList().addAll(response);
            }

            @Override
            public void onFailure(ServerFailure e) {
                messageDisplay.error(SRC, "Error loading trace templates: ", e);
            }
        });
    }


}
