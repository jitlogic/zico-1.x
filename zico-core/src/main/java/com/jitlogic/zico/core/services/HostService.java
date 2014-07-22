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

import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.UserManager;
import com.jitlogic.zico.shared.data.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Path("/hosts")
public class HostService {

    private final static Logger log = LoggerFactory.getLogger(HostService.class);

    @Inject
    private HostStoreManager hostStoreManager;

    @Inject
    private UserManager userManager;

    @Inject
    private UserContext userContext;


    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<HostInfo> list() {
        List<HostStore> hostList = hostStoreManager.list(userContext.isInRole("ADMIN") ? null
                : userContext.getUser().getAllowedHosts());

        List<HostInfo> lst = new ArrayList<>();

        for (HostStore h : hostList) {
            lst.add(HostService.toHostInfo(h));
        }

        return lst;
    }

    @PUT @Path("/{hostname}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("hostname")String hostname, HostInfo hi) {
        HostStore host = hostStoreManager.find(HostStore.class, hostname);
        if (host != null) {
            host.update(hi);
            hostStoreManager.persist(host);
        }
    }

    @POST @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(HostInfo hi) {
        hostStoreManager.newHost(hi.getName(), hi.getAddr(), hi.getGroup(),
                hi.getComment(), hi.getPass(), hi.getMaxSize());
    }

    @DELETE @Path("/{hostname}")
    public void delete(@PathParam("hostname") String hostname) {
        try {
            hostStoreManager.delete(hostname);
        } catch (IOException e) {
            log.error("Cannot remove host '" + hostname + "'", e);
        }
    }

    public static HostInfo toHostInfo(HostStore host) {
        HostInfo hi = new HostInfo();

        if (host != null) {
            hi.setName(host.getName());
            hi.setAddr(host.getAddr());
            hi.setComment(host.getComment());
            hi.setFlags(host.getFlags());
            hi.setEnabled(host.isEnabled());
            hi.setMaxSize(host.getMaxSize());
            hi.setGroup(host.getGroup());
            hi.setComment(host.getComment());
        }

        return hi;
    }

}
