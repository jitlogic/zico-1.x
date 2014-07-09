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
package com.jitlogic.zico.test.support;

import com.google.inject.Singleton;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.ZicoRuntimeException;
import com.jitlogic.zico.shared.data.UserInfo;

import javax.inject.Inject;

@Singleton
public class UserTestContext implements UserContext {

    public UserInfo user;

    public boolean isAdmin = true;

    @Inject
    public UserTestContext() {
        user = new UserInfo();
        user.setAdmin(true);
        user.setUserName("test");
        user.setRealName("Test User");
    }

    @Override
    public UserInfo getUser() {
        return user;
    }

    @Override
    public boolean isInRole(String role) {
        return "ADMIN".equals(role) ? isAdmin : true;
    }

    @Override
    public void checkAdmin() {
        if (!isAdmin) {
            throw new ZicoRuntimeException("Insufficient privileges");
        }
    }
}
