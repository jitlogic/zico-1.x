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
package com.jitlogic.zico.test.support;


import com.google.inject.Binder;
import com.google.inject.Provides;
import com.jitlogic.zico.core.DBFactory;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.inject.AbstractZicoModule;

import javax.inject.Singleton;

public class TestZicoModule extends AbstractZicoModule {

    private ZicoConfig config;

    public TestZicoModule(ZicoConfig config) {
        this.config = config;
    }

    @Override
    public void configure(Binder binder) {
        super.configure(binder);
        binder.bind(UserContext.class).to(UserTestContext.class);
        binder.bind(DBFactory.class).to(MemoryDBFactory.class);
    }


    @Provides
    @Singleton
    public ZicoConfig provideConfig() {
        return config;
    }


}
