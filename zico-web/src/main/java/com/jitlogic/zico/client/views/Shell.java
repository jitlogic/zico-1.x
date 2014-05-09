package com.jitlogic.zico.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
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

    ErrorHandler errorHandler;

    @Inject
    public Shell(final HostListPanel hostListPanel, ZicoRequestFactory rf,
                 ErrorHandler errorHandler, WelcomeView welcomeView) {
        this.hostListPanel = hostListPanel;
        this.welcomeView = welcomeView;
        this.errorHandler = errorHandler;
        initWidget(ourUiBinder.createAndBindUi(this));

        rf.userService().isAdminMode().fire(new Receiver<Boolean>() {
            @Override
            public void onSuccess(Boolean response) {
                hostListPanel.setAdminMode(response);
            }
            @Override
            public void onFailure(ServerFailure error) {
                Shell.this.errorHandler.error("Error", error);
            }
        });

    }

    public void addView(Widget widget, String title) {
        tabPanel.add(widget, title);
        tabPanel.selectTab(widget);
    }
}