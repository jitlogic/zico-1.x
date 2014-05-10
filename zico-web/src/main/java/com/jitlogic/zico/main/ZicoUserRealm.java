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
package com.jitlogic.zico.main;



import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZicoUserRealm implements LoginService {

    private static ZicoUserRealm instance;

    public static ZicoUserRealm getInstance() {
        return instance;
    }

    private String name;
    private Map<String,ZicoUser> users;


    public ZicoUserRealm() {
        users = new ConcurrentHashMap<String, ZicoUser>();
        ZicoUserRealm.instance = this;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public UserIdentity login(String username, Object credentials) {
        ZicoUser user = users.get(username);

        if (user != null) {
            if (user.authenticate(credentials)) {
                user.setAuthenticated(true);
                return user;
            }
        }

        return null;
    }

    @Override
    public boolean validate(UserIdentity user) {
        return false;
    }

    @Override
    public IdentityService getIdentityService() {
        return null;
    }

    @Override
    public void setIdentityService(IdentityService service) {

    }

    @Override
    public void logout(UserIdentity user) {

    }


    public void setName(String name) {
        this.name = name;
    }


    public void update(String username, String password, Boolean isAdmin) {
        users.put(username, new ZicoUser(username, password, isAdmin));
    }


    public void remove(String username) {
        users.remove(username);
    }
}
