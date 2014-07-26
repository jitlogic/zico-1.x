package com.jitlogic.zico.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.jitlogic.zico.client.api.SystemService;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.views.hosts.HostListPanel;
import com.jitlogic.zico.shared.data.UserInfo;
import com.jitlogic.zico.widgets.client.*;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

public class Shell extends Composite {
    interface ShellUiBinder extends UiBinder<Widget, Shell> { }
    private static ShellUiBinder ourUiBinder = GWT.create(ShellUiBinder.class);

    @UiField(provided = true)
    HostListPanel hostListPanel;

    @UiField
    TabLayoutPanel tabPanel;

    @UiField(provided = true)
    WelcomeView welcomeView;

    @UiField
    Hyperlink lnkAdmin;

    @UiField
    Hyperlink lnkUser;

    @UiField(provided = true)
    StatusBar statusBar;

    private SystemService systemService;
    private PanelFactory panelFactory;

    private final String SRC = "Shell";

    private PopupMenu userMenu;
    private PopupMenu adminMenu;


    @Inject
    public Shell(final HostListPanel hostListPanel,
                 SystemService systemService, WelcomeView welcomeView,
                 PanelFactory panelFactory, MessageDisplay md) {
        this.hostListPanel = hostListPanel;
        this.welcomeView = welcomeView;
        this.systemService = systemService;
        this.panelFactory = panelFactory;
        this.statusBar = (StatusBar)md;

        initWidget(ourUiBinder.createAndBindUi(this));

        checkAdminRole();
        createUserMenu();
        createAdminMenu();
    }


    public void addView(Widget widget, String caption) {
        tabPanel.add(widget, new CloseableTab(tabPanel, widget, caption));
        tabPanel.selectTab(widget);
    }


    private void checkAdminRole() {
        statusBar.info(SRC, "Loading user profile ...");

        systemService.user(new MethodCallback<UserInfo>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                statusBar.error(SRC, "Error performing server request", e);
            }

            @Override
            public void onSuccess(Method method, UserInfo user) {
                hostListPanel.setAdminMode(user.isAdmin());
                lnkAdmin.setVisible(user.isAdmin());
                statusBar.clear(SRC);

                lnkUser.setText(user.getUserName() + " (" + user.getRealName() + ") " +
                        (user.isAdmin() ? "ADMIN" : "VIEWER"));
            }
        });
    }


    private void createAdminMenu() {
        adminMenu = new PopupMenu();
        adminMenu.addItem(new MenuItem("Manage users", Resources.INSTANCE.usersIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        addView(panelFactory.userManagementPanel(), "User Management");
                    }
                }));
        adminMenu.addItem(new MenuItem("Templates", Resources.INSTANCE.listColumnsIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        addView(panelFactory.traceTemplatePanel(), "Templates");
                    }
                }));
        adminMenu.addSeparator();
        adminMenu.addItem(new MenuItem("Backup configuration", Resources.INSTANCE.backupIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        doBackup();
                    }
                }));
    }


    private void createUserMenu() {
        userMenu = new PopupMenu();
        userMenu.addItem(new MenuItem("Change password", Resources.INSTANCE.keyIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        panelFactory.passwordChangeView("", false).asPopupWindow().show();
                    }
                }));
        userMenu.addSeparator();
        userMenu.addItem(new MenuItem("Logout", Resources.INSTANCE.logoutIcon(),
                new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        Window.Location.assign(GWT.getHostPageBaseURL() + "logout");
                    }
                }));
    }


    private void doBackup() {
        systemService.backup(new MethodCallback<Void>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                statusBar.error(SRC, "Error backing up symbols and configuration.", e);
            }

            @Override
            public void onSuccess(Method method, Void response) {
                Window.alert("Symbols and configuration backed up succesfully.");
            }
        });
    }


    @UiHandler("lnkAdmin")
    void doAdmin(ClickEvent e) {
        adminMenu.setPopupPosition(
                Math.max(5, e.getNativeEvent().getClientX()-25),
                Math.max(0, e.getNativeEvent().getClientY()-10));
        adminMenu.show();
    }


    @UiHandler("lnkUser")
    void doLogout(ClickEvent e) {
        userMenu.setPopupPosition(
                Math.min(e.getNativeEvent().getClientX()-25, Window.getClientWidth()-160),
                Math.max(0, e.getNativeEvent().getClientY()-10));
        userMenu.show();
    }

}

