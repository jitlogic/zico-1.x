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
package com.jitlogic.zico.core;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class UserHttpContext implements UserContext {

    private String anonymous;
    private Provider<HttpServletRequest> req;

    @Inject
    public UserHttpContext(Provider<HttpServletRequest> req, ZicoConfig config) {
        this.anonymous = config.stringCfg("zico.anonymous", null);
        this.req = req;
    }

    @Override
    public String getUser() {
        if (anonymous != null) { return anonymous; }
        return req.get().getRemoteUser();
    }

    @Override
    public boolean isInRole(String role) {
        if (anonymous != null) { return true; }
        HttpServletRequest r = req.get();
        return r == null || r.isUserInRole(role);
    }

    @Override
    public void checkAdmin() {
        if (!isInRole("ADMIN")) {
            throw new ZicoRuntimeException("Insufficient privileges.");
        }
    }

}
