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

import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

import javax.security.auth.Subject;
import java.security.Principal;

public class ZicoUser implements Principal, UserIdentity {

    private String username;
    private Credential credential;
    private boolean admin, authenticated;
    private transient Subject subject;

    public ZicoUser(String username, String password, boolean admin) {
        this.username = username;
        credential = Credential.getCredential(password != null ? password : "");
        this.admin = admin;
        Subject subject = new Subject();
        subject.getPrincipals().add(this);
        subject.getPrivateCredentials().add(credential);
    }

    @Override
    public String getName() {
        return username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public synchronized boolean isAuthenticated() {
        return authenticated;
    }

    public synchronized void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public synchronized boolean authenticate(Object pass) {
        return credential.check(pass);
    }


    @Override
    public Subject getSubject() {
        return subject;
    }


    @Override
    public Principal getUserPrincipal() {
        return this;
    }


    @Override
    public boolean isUserInRole(String role, Scope scope) {
        return false;
    }
}
