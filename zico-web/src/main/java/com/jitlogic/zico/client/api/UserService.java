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

package com.jitlogic.zico.client.api;

import com.jitlogic.zico.shared.data.UserInfo;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import javax.ws.rs.*;
import java.util.List;

public interface UserService extends RestService {

    @GET
    @Path("users")
    void list(MethodCallback<List<UserInfo>> cb);

    @PUT
    @Path("users/{username}")
    void update(@PathParam("username") String username, UserInfo ui, MethodCallback<Void> cb);

    @POST
    @Path("users")
    void create(UserInfo user, MethodCallback<Void> cb);

    @DELETE @Path("users/{username}")
    void delete(@PathParam("username") String username, MethodCallback<Void> cb);
}
