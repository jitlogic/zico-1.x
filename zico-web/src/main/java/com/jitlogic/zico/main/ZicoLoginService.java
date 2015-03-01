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
package com.jitlogic.zico.main;



import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Principal;

public class ZicoLoginService extends MappedLoginService {

    private static ZicoLoginService instance;

    public static ZicoLoginService getInstance() {
        return instance;
    }


    public ZicoLoginService() {
        setName("Zorka Intranet Collector");
        ZicoLoginService.instance = this;
    }


    @Override
    protected UserIdentity loadUser(String username) {
        return null;
    }


    @Override
    protected void loadUsers() throws IOException {
    }

    public void update(String username, String credentials, Boolean isAdmin) {
        String[] roleArray = isAdmin ? new String[] { "ADMIN", "VIEWER" } : new String[] { "VIEWER" };
        Credential credential = Credential.getCredential(credentials);
        Principal userPrincipal = new KnownUser(username,credential);
        Subject subject = new Subject();
        subject.getPrincipals().add(userPrincipal);
        subject.getPrivateCredentials().add(credential);
        _users.put(username, _identityService.newUserIdentity(subject,userPrincipal,roleArray));
        for (String role : roleArray) {
            subject.getPrincipals().add(new RolePrincipal(role));
        }
    }

    public void remove(String username) {
        super.removeUser(username);
    }
}
