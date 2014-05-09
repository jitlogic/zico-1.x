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
package com.jitlogic.zico.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;

import javax.inject.Provider;
import java.util.List;

public class WelcomeView extends Composite {
    interface WelcomeViewUiBinder extends UiBinder<HTMLPanel, WelcomeView> { }
    private static WelcomeViewUiBinder ourUiBinder = GWT.create(WelcomeViewUiBinder.class);

    @UiField
    VerticalPanel infoPanel;

    @UiField
    Hyperlink lnkChangePassword;

    @UiField
    Hyperlink lnkManageUsers;

    @UiField
    Hyperlink lnkBackup;

    @UiField
    Hyperlink lnkTraceDisplayTemplates;

    private ZicoRequestFactory rf;

    private Provider<Shell> shell;

    private PanelFactory panelFactory;

    private ErrorHandler errorHandler;

    private Timer timer;

    @Inject
    public WelcomeView(final ZicoRequestFactory rf,
                       final PanelFactory panelFactory, final Provider<Shell> shell,
                       final ErrorHandler errorHandler) {

        this.rf = rf;
        this.panelFactory = panelFactory;
        this.shell = shell;
        this.errorHandler = errorHandler;

        initWidget(ourUiBinder.createAndBindUi(this));

        rf.userService().isAdminMode().fire(new Receiver<Boolean>() {
            @Override
            public void onSuccess(Boolean isAdmin) {
                if (!isAdmin) {
                    lnkBackup.setVisible(false);
                    lnkManageUsers.setVisible(false);
                    lnkTraceDisplayTemplates.setVisible(false);
                } else {
                    lnkTraceDisplayTemplates.addHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            openTemplatePanel();
                        }
                    }, ClickEvent.getType());

                }
            }
            @Override
            public void onFailure(ServerFailure failure) {
                WelcomeView.this.errorHandler.error("Error performing server request", failure);
            }
        });

        lnkTraceDisplayTemplates.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openTemplatePanel();
            }
        }, ClickEvent.getType());

        lnkManageUsers.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                shell.get().addView(panelFactory.userManagementPanel(), "User Management");
            }
        }, ClickEvent.getType());

        lnkBackup.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
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
        }, ClickEvent.getType());

        lnkChangePassword.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                panelFactory.passwordChangeDialog("").show();
            }
        }, ClickEvent.getType());

        timer = new Timer() {
            @Override
            public void run() {
                loadData();
            }
        };
        timer.scheduleRepeating(10000);

    }

    private void openTemplatePanel() {
        shell.get().addView(panelFactory.traceTemplatePanel(), "Templates");
    }

    private void loadData() {
        rf.systemService().systemInfo().fire(new Receiver<List<String>>() {
            @Override
            public void onSuccess(List<String> response) {
                infoPanel.clear();
                for (String s : response) {
                    infoPanel.add(new Label(s));
                }
            }

            @Override
            public void onFailure(ServerFailure e) {
                infoPanel.clear();
                infoPanel.add(new Label("Error: " + e.getMessage()));
            }
        });
    }

}
