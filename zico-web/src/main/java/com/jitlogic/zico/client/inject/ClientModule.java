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
package com.jitlogic.zico.client.inject;


import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.inject.Provides;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.api.*;
import com.jitlogic.zico.client.views.Shell;
import com.jitlogic.zico.client.views.StatusBar;
import org.fusesource.restygwt.client.Resource;
import org.fusesource.restygwt.client.RestServiceProxy;

import javax.inject.Singleton;

public class ClientModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(EventBus.class).to(SimpleEventBus.class);
        bind(Shell.class).in(Singleton.class);
        bind(MessageDisplay.class).to(StatusBar.class).in(Singleton.class);
        install(new GinFactoryModuleBuilder().build(PanelFactory.class));
    }

    @Provides @Singleton
    HostService provideHostService() {
        HostService hostService = GWT.create(HostService.class);
        ((RestServiceProxy)hostService).setResource(new Resource(GWT.getHostPageBaseURL() + "data"));
        return hostService;
    }

    @Provides @Singleton
    SystemService provideSystemService() {
        SystemService systemService = GWT.create(SystemService.class);
        ((RestServiceProxy)systemService).setResource(new Resource(GWT.getHostPageBaseURL() + "data"));
        return systemService;
    }

    @Provides @Singleton
    UserService provideUserService() {
        UserService userService = GWT.create(UserService.class);
        ((RestServiceProxy)userService).setResource(new Resource(GWT.getHostPageBaseURL() + "data"));
        return userService;
    }

    @Provides @Singleton
    TraceTemplateService provideTraceTemplateService() {
        TraceTemplateService templateService = GWT.create(TraceTemplateService.class);
        ((RestServiceProxy)templateService).setResource(new Resource(GWT.getHostPageBaseURL() + "data"));
        return templateService;
    }

    @Provides @Singleton
    TraceDataService provideTraceDataService() {
        TraceDataService traceDataService = GWT.create(TraceDataService.class);
        ((RestServiceProxy)traceDataService).setResource(new Resource(GWT.getHostPageBaseURL() + "data"));
        return traceDataService;
    }
}
