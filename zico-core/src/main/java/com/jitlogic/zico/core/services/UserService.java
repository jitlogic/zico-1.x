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

package com.jitlogic.zico.core.services;

import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.UserManager;
import com.jitlogic.zico.shared.data.UserInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;


@Singleton
@Path("/users")
public class UserService {

    @Inject
    private UserManager userManager;

    @Inject
    private UserContext userContext;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserInfo> list() {
        userContext.checkAdmin();

        return userManager.findAll();
    }

    @GET
    @Path("/{username}")
    public UserInfo get(@PathParam("username") String username) {
        userContext.checkAdmin();
        return userManager.find(username);
    }

    @PUT
    @Path("/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("username") String username, UserInfo userInfo) {
        userContext.checkAdmin();
        UserInfo user = userManager.find(username);
        if (user != null) {
            userManager.persist(userInfo);
        }
    }


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(UserInfo userInfo) {
        userManager.persist(userInfo);
    }


    @DELETE
    @Path("/{username}")
    public void delete(@PathParam("username")String username) {
        userContext.checkAdmin();
        userManager.remove(userManager.find(username));
    }

}
