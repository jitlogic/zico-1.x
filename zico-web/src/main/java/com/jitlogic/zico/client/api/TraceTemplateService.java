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

package com.jitlogic.zico.client.api;

import com.jitlogic.zico.shared.data.TraceTemplateInfo;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import javax.ws.rs.*;
import java.util.List;

public interface TraceTemplateService extends RestService {

    @GET
    @Path("templates")
    void list(MethodCallback<List<TraceTemplateInfo>> cb);


    @PUT
    @Path("users/{tid}")
    void update(@PathParam("tid") int tid, TraceTemplateInfo ti, MethodCallback<Void> cb);


    @POST
    @Path("templates")
    void create(TraceTemplateInfo ti, MethodCallback<Void> cb);


    @DELETE @Path("templates/{tid}")
    void delete(@PathParam("tid") int tid, MethodCallback<Void> cb);
}
