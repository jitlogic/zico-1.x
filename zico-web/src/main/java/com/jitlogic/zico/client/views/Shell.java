package com.jitlogic.zico.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.jitlogic.zico.client.ErrorHandler;
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

    private ErrorHandler errorHandler;

    private ZicoRequestFactory rf;

    private PanelFactory panelFactory;

    @Inject
    public Shell(final HostListPanel hostListPanel, ZicoRequestFactory rf,
                 ErrorHandler errorHandler, WelcomeView welcomeView,
                 PanelFactory panelFactory) {
        this.hostListPanel = hostListPanel;
        this.welcomeView = welcomeView;
        this.errorHandler = errorHandler;
        this.panelFactory = panelFactory;
        this.rf = rf;

        initWidget(ourUiBinder.createAndBindUi(this));
        checkAdminRole();
    }


    public void addView(Widget widget, String title) {
        tabPanel.add(widget, title);
        tabPanel.selectTab(widget);
    }


    private void checkAdminRole() {
        rf.userService().isAdminMode().fire(new Receiver<Boolean>() {
            @Override
            public void onSuccess(Boolean isAdmin) {
                hostListPanel.setAdminMode(isAdmin);
                if (!isAdmin) {
                    lnkManageUsers.setVisible(false);
                    lnkBackupConfig.setVisible(false);
                    lnkTraceTemplates.setVisible(false);
                }
            }
            @Override
            public void onFailure(ServerFailure failure) {
                //WelcomeView.this.errorHandler.error("Error performing server request", failure);
                //TODO proper status bar here
            }
        });
    }


    @UiHandler("lnkManageUsers")
    void openUserManager(ClickEvent e) {
        addView(panelFactory.userManagementPanel(), "User Management");
    }


    @UiHandler("lnkManageUsers")
    void openUserManagementPanel(ClickEvent e) {
        rf.systemService().backupConfig().fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void response) {
                Window.alert("Symbols and configuration backed up succesfully.");
            }

            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error backing up symbols and configuration.", failure);
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

