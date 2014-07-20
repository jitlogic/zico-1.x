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

import com.jitlogic.zico.shared.data.UserInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class UserHttpContext implements UserContext {

    private UserInfo anonymous;
    private UserManager userManager;

    @Inject
    public UserHttpContext(ZicoConfig config, UserManager userManager) {
        String mode = config.stringCfg("auth", "form");
        this.userManager = userManager;

        if ("anonymous".equals(mode)) {
            anonymous = new UserInfo();
            anonymous.setAdmin(true);
            anonymous.setUserName("anonymous");
            anonymous.setRealName("Anonymous");
        }
    }


    @Override
    public UserInfo getUser() {
        if (anonymous != null) { return anonymous; }


        if (ZicoRequestContextFilter.getRequest() != null) {
            String username = ZicoRequestContextFilter.getRequest().getRemoteUser();

            if (username != null) {
                UserInfo user = userManager.find(username);
                if (user != null) {
                    return user;
                }
            }
        }

        throw new ZicoRuntimeException("Cannot determine logged in user.");
    }


    @Override
    public boolean isInRole(String role) {
        if (anonymous != null) { return true; }
        UserInfo user = getUser();
        return user != null && (!"ADMIN".equals(role) || user.isAdmin());
    }
    

    @Override
    public void checkAdmin() {
        if (!isInRole("ADMIN")) {
            throw new ZicoRuntimeException("Insufficient privileges.");
        }
    }


    public void checkHostAccess(String hostname) {
        if (!isInRole("ADMIN")
                && !getUser().getAllowedHosts().contains(hostname)) {
            throw new ZicoRuntimeException("Insufficient privileges");
        }
    }



}
