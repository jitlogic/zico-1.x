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
package com.jitlogic.zico.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import com.jitlogic.zico.client.inject.ClientGinjector;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.client.views.Shell;
import com.jitlogic.zico.widgets.client.WidgetResources;
import com.jitlogic.zico.widgets.client.ZicoDataGridResources;


public class ZicoEP implements EntryPoint {

    static {
        ZicoDataGridResources.INSTANCE.dataGridStyle().ensureInjected();
        WidgetResources.INSTANCE.formCss().ensureInjected();
        WidgetResources.INSTANCE.toolBarCss().ensureInjected();
        Resources.INSTANCE.zicoCssResources().ensureInjected();
    }

    private Shell shell;
    private ClientGinjector injector = GWT.create(ClientGinjector.class);

    public void onModuleLoad() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                Window.enableScrolling(false);
                shell = injector.getShell();
                RootLayoutPanel.get().add(shell);
            }
        });
    }
}
