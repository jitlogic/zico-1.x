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

import com.jitlogic.zico.core.TraceTemplateManager;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.model.TraceTemplate;
import com.jitlogic.zico.shared.data.TraceTemplateInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        List<TraceTemplateInfo> lst = new ArrayList<>();

        for (TraceTemplate tmpl : templateManager.listTemplates()) {
            lst.add(TraceTemplateService.toTemplateInfo(tmpl));
        }

        return lst;
    }


    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TraceTemplateInfo get(@PathParam("id")int id) {
        return toTemplateInfo(templateManager.find(TraceTemplate.class, id));
    }


    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") int id, TraceTemplateInfo ti) {
        TraceTemplate t = templateManager.find(TraceTemplate.class, id);
        if (t != null) {
            templateManager.save(updateTemplate(t, ti));
        }
    }


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(TraceTemplateInfo ti) {
        TraceTemplate t = templateManager.create(TraceTemplate.class);
        templateManager.save(updateTemplate(t, ti));
    }


    @DELETE @Path("/{id}")
    public void delete(@PathParam("id") int id) {
        templateManager.remove(id);
    }


    public static TraceTemplate updateTemplate(TraceTemplate t, TraceTemplateInfo ti) {
        t.setOrder(ti.getOrder());
        t.setFlags(ti.getFlags());
        t.setCondition(ti.getCondition());
        t.setTemplate(ti.getTemplate());
        return t;
    }


    public static TraceTemplateInfo toTemplateInfo(TraceTemplate t) {
        TraceTemplateInfo ti = new TraceTemplateInfo();

        if (t != null) {
            ti.setId(t.getId());
            ti.setFlags(t.getFlags());
            ti.setOrder(t.getOrder());
            ti.setCondition(t.getCondition());
            ti.setTemplate(t.getTemplate());
        }

        return ti;
    }
}
