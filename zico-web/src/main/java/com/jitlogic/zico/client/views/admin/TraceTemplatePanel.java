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
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
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
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.widgets.*;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.TraceTemplateProxy;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class TraceTemplatePanel extends Composite {
    interface TraceTemplatePanelUiBinder extends UiBinder<Widget, TraceTemplatePanel> { }
    private static TraceTemplatePanelUiBinder ourUiBinder = GWT.create(TraceTemplatePanelUiBinder.class);

    @UiField
    DockLayoutPanel panel;

    @UiField
    ToolButton btnRefresh;

    @UiField
    ToolButton btnAdd;

    @UiField
    ToolButton btnEdit;

    @UiField
    ToolButton btnRemove;

    @UiField(provided = true)
    DataGrid<TraceTemplateProxy> templateGrid;


    private ListDataProvider<TraceTemplateProxy> templateStore;
    private SingleSelectionModel<TraceTemplateProxy> selectionModel;

    private ZicoRequestFactory rf;

    private PopupMenu contextMenu;

    private MessageDisplay md;

    private final static String MDS = "TraceTemplatePanel";

    @Inject
    public TraceTemplatePanel(ZicoRequestFactory rf, MessageDisplay md) {

        this.md = md;
        this.rf = rf;

        createTemplateListGrid();
        ourUiBinder.createAndBindUi(this);
        initWidget(panel);

        createContextMenu();

        refreshTemplates(null);
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
        templateGrid = new DataGrid<TraceTemplateProxy>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
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
                    editTemplate(null);
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
    }


    private void createContextMenu() {
        contextMenu = new PopupMenu();

        MenuItem mnuRefresh = new MenuItem("Refresh", Resources.INSTANCE.refreshIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        refreshTemplates(null);
                    }
                });
        contextMenu.addItem(mnuRefresh);

        contextMenu.addSeparator();

        MenuItem mnuCreateTemplate = new MenuItem("New template", Resources.INSTANCE.addIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        addTemplate(null);
                    }
                });
        contextMenu.addItem(mnuCreateTemplate);

        MenuItem mnuRemoveTemplate = new MenuItem("Remove template", Resources.INSTANCE.removeIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        removeTemplate(null);
                    }
                });
        contextMenu.addItem(mnuRemoveTemplate);

        contextMenu.addSeparator();

        MenuItem mnuEditTemplate = new MenuItem("Edit Template", Resources.INSTANCE.editIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        editTemplate(null);
                    }
                });
        contextMenu.addItem(mnuEditTemplate);
    }


    @UiHandler("btnEdit")
    void editTemplate(ClickEvent e) {
        TraceTemplateProxy tti = selectionModel.getSelectedObject();
        if (tti != null) {
            new TraceTemplateEditDialog(rf, this, tti, md).asPopupWindow().show();
        }
    }


    @UiHandler("btnAdd")
    void addTemplate(ClickEvent e) {
        new TraceTemplateEditDialog(rf, this, null, md).asPopupWindow().show();
    }


    @UiHandler("btnRemove")
    void removeTemplate(ClickEvent e) {
        final TraceTemplateProxy template = selectionModel.getSelectedObject();
        if (template != null) {
            ConfirmDialog dialog = new ConfirmDialog("Removing template", "Remove template ?")
                    .withBtn("Yes", new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            rf.systemService().removeTemplate(template.getId()).fire(
                                    new Receiver<Void>() {
                                        @Override
                                        public void onSuccess(Void response) {
                                            refreshTemplates(null);
                                        }
                                    });

                        }
                    }).withBtn("No");
            dialog.show();
        }
    }


    @UiHandler("btnRefresh")
    public void refreshTemplates(ClickEvent e) {
        md.info(MDS, "Loading trace display templates");
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
                md.clear(MDS);
            }

            @Override
            public void onFailure(ServerFailure e) {
                md.error(MDS, "Error loading trace templates: ", e);
            }
        });
    }


}
