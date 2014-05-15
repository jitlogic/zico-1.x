package com.jitlogic.zico.client.views;

import com.google.gwt.core.client.GWT;
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
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.views.hosts.HostListPanel;

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
    Hyperlink lnkManageUsers;

    @UiField
    Hyperlink lnkChangePassword;

    @UiField
    Hyperlink lnkBackupConfig;

    @UiField
    Hyperlink lnkTraceTemplates;

    @UiField(provided = true)
    StatusBar statusBar;

    private ZicoRequestFactory rf;

    private PanelFactory panelFactory;

    private final String SRC = "Shell";

    @Inject
    public Shell(final HostListPanel hostListPanel, ZicoRequestFactory rf,
                 WelcomeView welcomeView,
                 PanelFactory panelFactory, MessageDisplay md) {
        this.hostListPanel = hostListPanel;
        this.welcomeView = welcomeView;
        this.panelFactory = panelFactory;
        this.statusBar = (StatusBar)md;
        this.rf = rf;

        initWidget(ourUiBinder.createAndBindUi(this));
        checkAdminRole();
    }


    public void addView(Widget widget, String title) {
        tabPanel.add(widget, title);
        tabPanel.selectTab(widget);
    }


    private void checkAdminRole() {
        statusBar.info(SRC, "Loading user profile ...");
        rf.userService().isAdminMode().fire(new Receiver<Boolean>() {
            @Override
            public void onSuccess(Boolean isAdmin) {
                hostListPanel.setAdminMode(isAdmin);
                if (!isAdmin) {
                    lnkManageUsers.setVisible(false);
                    lnkBackupConfig.setVisible(false);
                    lnkTraceTemplates.setVisible(false);
                }
                statusBar.clear(SRC);
            }
            @Override
            public void onFailure(ServerFailure failure) {
                statusBar.error(SRC, "Error performing server request", failure);
            }
        });
    }


    @UiHandler("lnkManageUsers")
    void openUserManager(ClickEvent e) {
        addView(panelFactory.userManagementPanel(), "User Management");
    }


    @UiHandler("lnkBackupConfig")
    void openUserManagementPanel(ClickEvent e) {
        rf.systemService().backupConfig().fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void response) {
                Window.alert("Symbols and configuration backed up succesfully.");
            }

            @Override
            public void onFailure(ServerFailure failure) {
                statusBar.error(SRC, "Error backing up symbols and configuration.", failure);
            }
        });
    }


    @UiHandler("lnkTraceTemplates")
    void openTemplatePanel(ClickEvent e) {
        addView(panelFactory.traceTemplatePanel(), "Templates");
    }


    @UiHandler("lnkChangePassword")
    void changePassword(ClickEvent e) {
        panelFactory.passwordChangeView("").asPopupWindow().show();
    }


    @UiHandler("lnkLogOut")
    void logOut(ClickEvent e) {
        Window.alert("Not implemented yet.");
    }
}

