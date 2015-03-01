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

package com.jitlogic.zico.core.services;

import com.jitlogic.zico.core.TraceTemplateManager;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.shared.data.TraceTemplateInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Path("/templates")
public class TraceTemplateService {

    @Inject
    private TraceTemplateManager templateManager;

    @Inject
    private UserContext userContext;


    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TraceTemplateInfo> list() {
        userContext.checkAdmin();

        return templateManager.listTemplates();
    }


    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TraceTemplateInfo get(@PathParam("id")int id) {
        return templateManager.find(TraceTemplateInfo.class, id);
    }


    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") int id, TraceTemplateInfo ti) {
        templateManager.save(ti);
    }


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(TraceTemplateInfo ti) {
        templateManager.save(ti);
    }


    @DELETE @Path("/{id}")
    public void delete(@PathParam("id") int id) {
        templateManager.remove(id);
    }
}
