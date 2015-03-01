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
package com.jitlogic.zico.core.inject;


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.ZicoRequestContextFilter;
import com.jitlogic.zico.core.ZicoService;
import com.jitlogic.zico.core.services.*;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;

import javax.servlet.http.HttpServletRequest;


public abstract class AbstractZicoModule implements Module {


    @Override
    public void configure(Binder binder) {
        binder.bind(ZicoDataProcessorFactory.class).to(HostStoreManager.class);

        binder.bind(HostService.class);
        binder.bind(SystemService.class);
        binder.bind(TraceTemplateService.class);
        binder.bind(TraceDataService.class);
        binder.bind(UserService.class);
    }


    @Provides
    @Singleton
    public ZicoService provideZicoService(ZicoDataProcessorFactory zcf, ZicoConfig config) {
        return new ZicoService(zcf,
                config.stringCfg("zico.listen.addr", null),
                config.intCfg("zico.listen.port", null),
                config.intCfg("zico.threads.max", null),
                config.intCfg("zico.socket.timeout", null));
    }

    @Provides
    public HttpServletRequest provideServletRequest() {
        return ZicoRequestContextFilter.getRequest();
    }
}
