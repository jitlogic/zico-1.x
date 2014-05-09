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
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.widgets.ResizableHeader;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.widgets.MenuItem;
import com.jitlogic.zico.client.widgets.PopupMenu;
import com.jitlogic.zico.client.widgets.ToolButton;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.data.UserProxy;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.event.HideEvent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserManagementPanel extends Composite {

    private ZicoRequestFactory rf;
    private PanelFactory panelFactory;
    private ErrorHandler errorHandler;

    private ListDataProvider<UserProxy> userStore;
    private DataGrid<UserProxy> userGrid;
    private SingleSelectionModel<UserProxy> selectionModel;

    private PopupMenu contextMenu;

    private List<String> hostNames = new ArrayList<String>();

    private DockLayoutPanel panel;

    @Inject
    public UserManagementPanel(ZicoRequestFactory requestFactory, PanelFactory panelFactory, ErrorHandler errorHandler) {

        this.rf = requestFactory;
        this.panelFactory = panelFactory;
        this.errorHandler = errorHandler;

        panel = new DockLayoutPanel(Style.Unit.PX);
        initWidget(panel);

        loadHosts();
        createToolBar();
        createUserGrid();
        createContextMenu();
        refreshUsers();
    }


    private void createToolBar() {
        HorizontalPanel toolBar = new HorizontalPanel();

        ToolButton btnRefresh = new ToolButton(Resources.INSTANCE.refreshIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        refreshUsers();
                    }
                });
        //btnRefresh.setToolTip("Refresh user list");
        toolBar.add(btnRefresh);

        //toolBar.add(new SeparatorToolItem());

        ToolButton btnAdd = new ToolButton(Resources.INSTANCE.addIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        addUser();
                    }
                });
        //btnAdd.setToolTip("Add user");
        toolBar.add(btnAdd);

        ToolButton btnRemove = new ToolButton(Resources.INSTANCE.removeIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        removeUser();
                    }
                });
        //btnRemove.setToolTip("Remove user");
        toolBar.add(btnRemove);

        //toolBar.add(new SeparatorToolItem());

        ToolButton btnPassword = new ToolButton(Resources.INSTANCE.keyIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        changePassword();
                    }
                });
        //btnPassword.setToolTip("Change user password");
        toolBar.add(btnPassword);

        panel.addNorth(toolBar, 32);
    }


    private final static ProvidesKey<UserProxy> KEY_PROVIDER = new ProvidesKey<UserProxy>() {
        @Override
        public Object getKey(UserProxy item) {
            return item.getUserName();
        }
    };

    private static final Cell<UserProxy> USERNAME_CELL = new AbstractCell<UserProxy>() {
        @Override
        public void render(Context context, UserProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.getUserName()));
        }
    };

    private static final Cell<UserProxy> REALNAME_CELL = new AbstractCell<UserProxy>() {
        @Override
        public void render(Context context, UserProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.getRealName()));
        }
    };

    private static final Cell<UserProxy> USERROLE_CELL = new AbstractCell<UserProxy>() {
        @Override
        public void render(Context context, UserProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.isAdmin() ? "ADMIN" : "VIEWER"));
        }
    };

    private static final Cell<UserProxy> USERHOSTS_CELL = new AbstractCell<UserProxy>() {
        @Override
        public void render(Context context, UserProxy value, SafeHtmlBuilder sb) {
            if (value.isAdmin()) {
                sb.appendHtmlConstant(
                    "<span style=\"color: gray;\"> ** all hosts visible due to administrator privileges ** </span>");
            } else {
                List<String> hosts = value.getAllowedHosts();
                if (hosts != null) {
                    for (int i = 0; i < hosts.size(); i++) {
                        if (i > 0) {
                            sb.appendHtmlConstant(",");
                        }
                        sb.append(SafeHtmlUtils.fromString(hosts.get(i)));
                    }
                }
            }
        }
    };


    private void createUserGrid() {
        userGrid = new DataGrid<UserProxy>(1024 * 1024, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<UserProxy>(KEY_PROVIDER);
        userGrid.setSelectionModel(selectionModel);

        Column<UserProxy,UserProxy> colUsername = new IdentityColumn<UserProxy>(USERNAME_CELL);
        userGrid.addColumn(colUsername, new ResizableHeader<UserProxy>("Username", userGrid, colUsername));
        userGrid.setColumnWidth(colUsername, 128, Style.Unit.PX);

        Column<UserProxy,UserProxy> colUserRole = new IdentityColumn<UserProxy>(USERROLE_CELL);
        userGrid.addColumn(colUserRole, new ResizableHeader<UserProxy>("Role", userGrid, colUserRole));
        userGrid.setColumnWidth(colUserRole, 64, Style.Unit.PX);

        Column<UserProxy,UserProxy> colRealName = new IdentityColumn<UserProxy>(REALNAME_CELL);
        userGrid.addColumn(colRealName, new ResizableHeader<UserProxy>("Real Name", userGrid, colRealName));
        userGrid.setColumnWidth(colRealName, 256, Style.Unit.PX);

        Column<UserProxy,UserProxy> colUserHosts = new IdentityColumn<UserProxy>(USERHOSTS_CELL);
        userGrid.addColumn(colUserHosts, "Allowed hosts");
        userGrid.setColumnWidth(colUserHosts, 100, Style.Unit.PCT);

        userStore = new ListDataProvider<UserProxy>(KEY_PROVIDER);
        userStore.addDataDisplay(userGrid);

        userGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<UserProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<UserProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selectionModel.setSelected(event.getValue(), true);
                    editUser();
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

        userGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());

        userGrid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        panel.add(userGrid);
    }


    private void createContextMenu() {
        contextMenu = new PopupMenu();

        MenuItem mnuRefresh = new MenuItem("Refresh", Resources.INSTANCE.refreshIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        refreshUsers();
                    }
                });
        contextMenu.addItem(mnuRefresh);

        contextMenu.addSeparator();

        MenuItem mnuAddUser = new MenuItem("Add user", Resources.INSTANCE.addIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        addUser();
                    }
                });
        contextMenu.addItem(mnuAddUser);

        MenuItem mnuRemoveUser = new MenuItem("Remove user", Resources.INSTANCE.removeIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        removeUser();
                    }
                });
        contextMenu.addItem(mnuRemoveUser);

        MenuItem mnuEditUser = new MenuItem("Edit user", Resources.INSTANCE.editIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        editUser();
                    }
                });
        contextMenu.addItem(mnuEditUser);

        contextMenu.addSeparator();

        MenuItem mnuChangePassword = new MenuItem("Change password", Resources.INSTANCE.keyIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        changePassword();
                    }
                });
        contextMenu.addItem(mnuChangePassword);
    }


    private void editUser() {
        UserProxy user = selectionModel.getSelectedObject();
        if (user != null) {
            new UserPrefsDialog(rf, user, this, hostNames, errorHandler).show();
        }
    }


    private void addUser() {
        new UserPrefsDialog(rf, null, this, hostNames, errorHandler).show();
    }


    private void removeUser() {
        final UserProxy user = selectionModel.getSelectedObject();
        if (user != null) {
            ConfirmMessageBox cmb = new ConfirmMessageBox(
                    "Removing host", "Are you sure you want to remove " + user.getUserName() + " ?");
            cmb.addHideHandler(new HideEvent.HideHandler() {
                @Override
                public void onHide(HideEvent event) {
                    Dialog d = (Dialog) event.getSource();
                    if ("Yes".equals(d.getHideButton().getText())) {
                        userStore.getList().remove(user);
                        rf.userService().remove(user).fire(
                                new Receiver<Void>() {
                                    @Override
                                    public void onSuccess(Void response) {
                                        refreshUsers();
                                    }
                                    public void onFailure(ServerFailure failure) {
                                        errorHandler.error("Error removing user " + user.getUserName(), failure);
                                    }
                                }
                        );
                    }
                }
            });
            cmb.show();
        }
    }


    private void changePassword() {
        UserProxy user = selectionModel.getSelectedObject();
        if (user != null) {
            PasswordChangeDialog dialog = panelFactory.passwordChangeDialog(user.getUserName());
            dialog.show();
        }
    }


    public void refreshUsers() {
        userStore.getList().clear();
        rf.userService().findAll().fire(new Receiver<List<UserProxy>>() {
            @Override
            public void onSuccess(List<UserProxy> users) {
                userStore.getList().addAll(users);
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error loading user data", failure);
            }
        });
    }


    private void loadHosts() {
        rf.hostService().findAll().fire(new Receiver<List<HostProxy>>() {
            @Override
            public void onSuccess(List<HostProxy> hosts) {
                hostNames.clear();
                for (HostProxy h : hosts) {
                    hostNames.add(h.getName());
                }
                Collections.sort(hostNames);
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error loading user data", failure);
            }
        });
    }
}
