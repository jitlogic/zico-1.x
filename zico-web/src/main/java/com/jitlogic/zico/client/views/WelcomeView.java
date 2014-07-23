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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.api.SystemService;
import com.jitlogic.zico.shared.data.SystemInfo;
import com.jitlogic.zico.widgets.client.MessageDisplay;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;


public class WelcomeView extends Composite {
    interface WelcomeViewUiBinder extends UiBinder<HTMLPanel, WelcomeView> { }
    private static WelcomeViewUiBinder ourUiBinder = GWT.create(WelcomeViewUiBinder.class);

    @UiField
    Label lblSystemStatus1;

    @UiField
    Label lblSystemStatus2;

    private SystemService systemService;

    private Timer timer;

    private MessageDisplay md;

    private static final String MDS = "WelcomeView";

    @Inject
    public WelcomeView(SystemService systemService, MessageDisplay md) {

        this.md = md;
        this.systemService = systemService;

        initWidget(ourUiBinder.createAndBindUi(this));

        refresh();

        timer = new Timer() {
            @Override
            public void run() {
                refresh();
            }
        };
        timer.scheduleRepeating(10000);
    }


    private void refresh() {
        systemService.systemInfo(new MethodCallback<SystemInfo>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                md.error(MDS, "Cannot get system info", exception);
            }

            @Override
            public void onSuccess(Method method, SystemInfo response) {
                updateInfo(response);
            }
        });
    }

    private static final long MB = 1047576;

    private void updateInfo(SystemInfo info) {
        lblSystemStatus1.setText("System uptime: " + ClientUtil.formatSecDuration(info.getUptime()/1000));

        lblSystemStatus2.setText("Memory: "
                + info.getUsedNonHeapMem()/MB
                + "MB / " + info.getTotalHeapMem()/MB + "MB");
    }
}
