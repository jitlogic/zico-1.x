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
import com.jitlogic.zico.client.api.HostService;
import com.jitlogic.zico.client.api.UserService;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.shared.data.HostInfo;
import com.jitlogic.zico.shared.data.UserInfo;
import com.jitlogic.zico.widgets.client.*;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserManagementPanel extends Composite {
    interface UserManagementPanelUiBinder extends UiBinder<Widget, UserManagementPanel> { }
    private static UserManagementPanelUiBinder ourUiBinder = GWT.create(UserManagementPanelUiBinder.class);

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

    @UiField
    ToolButton btnPassword;

    @UiField(provided = true)
    DataGrid<UserInfo> userGrid;

    private UserService userService;
    private HostService hostService;

    private PanelFactory panelFactory;

    private ListDataProvider<UserInfo> userStore;
    private SingleSelectionModel<UserInfo> selectionModel;

    private PopupMenu contextMenu;

    private List<String> hostNames = new ArrayList<String>();


    private MessageDisplay md;

    private static final String MDS = "UserManagementPanel";

    @Inject
    public UserManagementPanel(UserService userService, HostService hostService, PanelFactory panelFactory, MessageDisplay md) {
        this.userService = userService;
        this.hostService = hostService;
        this.panelFactory = panelFactory;
        this.md = md;

        createUserGrid();
        ourUiBinder.createAndBindUi(this);

        initWidget(panel);

        loadHosts();
        createContextMenu();
        refreshUsers(null);
    }


    private final static ProvidesKey<UserInfo> KEY_PROVIDER = new ProvidesKey<UserInfo>() {
        @Override
        public Object getKey(UserInfo item) {
            return item.getUserName();
        }
    };

    private static final Cell<UserInfo> USERNAME_CELL = new AbstractCell<UserInfo>() {
        @Override
        public void render(Context context, UserInfo value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.getUserName()));
        }
    };

    private static final Cell<UserInfo> REALNAME_CELL = new AbstractCell<UserInfo>() {
        @Override
        public void render(Context context, UserInfo value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.getRealName()));
        }
    };

    private static final Cell<UserInfo> USERROLE_CELL = new AbstractCell<UserInfo>() {
        @Override
        public void render(Context context, UserInfo value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.isAdmin() ? "ADMIN" : "VIEWER"));
        }
    };

    private static final Cell<UserInfo> USERHOSTS_CELL = new AbstractCell<UserInfo>() {
        @Override
        public void render(Context context, UserInfo value, SafeHtmlBuilder sb) {
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
        userGrid = new DataGrid<UserInfo>(1024 * 1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<UserInfo>(KEY_PROVIDER);
        userGrid.setSelectionModel(selectionModel);

        Column<UserInfo,UserInfo> colUsername = new IdentityColumn<UserInfo>(USERNAME_CELL);
        userGrid.addColumn(colUsername, new ResizableHeader<UserInfo>("Username", userGrid, colUsername));
        userGrid.setColumnWidth(colUsername, 128, Style.Unit.PX);

        Column<UserInfo,UserInfo> colUserRole = new IdentityColumn<UserInfo>(USERROLE_CELL);
        userGrid.addColumn(colUserRole, new ResizableHeader<UserInfo>("Role", userGrid, colUserRole));
        userGrid.setColumnWidth(colUserRole, 64, Style.Unit.PX);

        Column<UserInfo,UserInfo> colRealName = new IdentityColumn<UserInfo>(REALNAME_CELL);
        userGrid.addColumn(colRealName, new ResizableHeader<UserInfo>("Real Name", userGrid, colRealName));
        userGrid.setColumnWidth(colRealName, 256, Style.Unit.PX);

        Column<UserInfo,UserInfo> colUserHosts = new IdentityColumn<UserInfo>(USERHOSTS_CELL);
        userGrid.addColumn(colUserHosts, "Allowed hosts");
        userGrid.setColumnWidth(colUserHosts, 100, Style.Unit.PCT);

        userStore = new ListDataProvider<UserInfo>(KEY_PROVIDER);
        userStore.addDataDisplay(userGrid);

        userGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<UserInfo>() {
            @Override
            public void onCellPreview(CellPreviewEvent<UserInfo> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selectionModel.setSelected(event.getValue(), true);
                    editUser(null);
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
    }


    private void createContextMenu() {
        contextMenu = new PopupMenu();

        MenuItem mnuRefresh = new MenuItem("Refresh", Resources.INSTANCE.refreshIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        refreshUsers(null);
                    }
                });
        contextMenu.addItem(mnuRefresh);

        contextMenu.addSeparator();

        MenuItem mnuAddUser = new MenuItem("Add user", Resources.INSTANCE.addIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        addUser(null);
                    }
                });
        contextMenu.addItem(mnuAddUser);

        MenuItem mnuRemoveUser = new MenuItem("Remove user", Resources.INSTANCE.removeIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        removeUser(null);
                    }
                });
        contextMenu.addItem(mnuRemoveUser);

        MenuItem mnuEditUser = new MenuItem("Edit user", Resources.INSTANCE.editIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        editUser(null);
                    }
                });
        contextMenu.addItem(mnuEditUser);

        contextMenu.addSeparator();

        MenuItem mnuChangePassword = new MenuItem("Change password", Resources.INSTANCE.keyIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        changePassword(null);
                    }
                });
        contextMenu.addItem(mnuChangePassword);
    }


    @UiHandler("btnEdit")
    void editUser(ClickEvent e) {
        UserInfo user = selectionModel.getSelectedObject();
        if (user != null) {
            new UserEditDialog(userService, user, this, hostNames, md).asPopupWindow().show();
        }
    }


    @UiHandler("btnAdd")
    void addUser(ClickEvent e) {
        new UserEditDialog(userService, null, this, hostNames, md).asPopupWindow().show();
    }


    @UiHandler("btnRemove")
    void removeUser(ClickEvent e) {
        final UserInfo user = selectionModel.getSelectedObject();
        if (user != null) {
            ConfirmDialog dialog = new ConfirmDialog("Removing user", "Remove user " + user.getUserName() + " ?")
                    .withBtn("Yes", new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            userStore.getList().remove(user);
                            md.info(MDS, "Removing user " + user.getUserName());
                            userService.delete(user.getUserName(), new MethodCallback<Void>() {
                                @Override
                                public void onFailure(Method method, Throwable e) {
                                    md.error(MDS, "Cannot remove user " + user.getUserName(), e);
                                }

                                @Override
                                public void onSuccess(Method method, Void response) {
                                    md.clear(MDS);
                                }
                            });
                        }})
                    .withBtn("No");
            dialog.show();
        }
    }


    @UiHandler("btnPassword")
    void changePassword(ClickEvent e) {
        UserInfo user = selectionModel.getSelectedObject();
        if (user != null) {
            PasswordChangeDialog dialog = panelFactory.passwordChangeView(user.getUserName(), true);
            dialog.asPopupWindow().show();
        }
    }


    @UiHandler("btnRefresh")
    void refreshUsers(ClickEvent e) {
        userStore.getList().clear();
        userService.list(new MethodCallback<List<UserInfo>>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error loading user data", e);
            }

            @Override
            public void onSuccess(Method method, List<UserInfo> users) {
                userStore.getList().addAll(users);
            }
        });
    }


    private void loadHosts() {
        hostService.list(new MethodCallback<List<HostInfo>>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error loading user data", e);
            }

            @Override
            public void onSuccess(Method method, List<HostInfo> hosts) {
                hostNames.clear();
                for (HostInfo h : hosts) {
                    hostNames.add(h.getName());
                }
                Collections.sort(hostNames);

            }
        });
    }

}
