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
package com.jitlogic.zico.core;

import com.jitlogic.zico.shared.data.HostInfo;
import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Singleton
public class HostStoreManager implements Closeable, ZicoDataProcessorFactory {

    private final static Logger log = LoggerFactory.getLogger(HostStoreManager.class);

    private String dataDir;

    private boolean enableSecurity;

    private Map<String, HostStore> storesByName = new HashMap<String, HostStore>();

    private ZicoConfig config;
    private TraceTemplateManager templater;
    private DBFactory dbf;

    @Inject
    public HostStoreManager(ZicoConfig config, TraceTemplateManager templater, DBFactory dbf) {

        this.config = config;
        this.templater = templater;
        this.dbf = dbf;

        this.dataDir = config.stringCfg("zico.data.dir", null);

        this.enableSecurity = config.boolCfg("zico.security", false);
    }


    public synchronized HostStore getHost(String name, boolean create) {

        if (storesByName.containsKey(name)) {
            return storesByName.get(name);
        }

        if (name == null) {
            throw new RuntimeException(
                "Cannot create host without name. Please check if zorka.hostname is set in your agent.");
        }

        File hostPath = new File(dataDir, ZicoUtil.safePath(name));

        if (create || new File(hostPath, HostStore.HOST_PROPERTIES).exists()) {
            HostStore host = new HostStore(dbf, config, templater, name);
            storesByName.put(name, host);
            return host;
        }

        return null;
    }


    @Override
    public synchronized void close() throws IOException {
        for (Map.Entry<String, HostStore> entry : storesByName.entrySet()) {
            entry.getValue().close();
        }
        storesByName.clear();
    }


    @Override
    public ZicoDataProcessor get(Socket socket, HelloRequest hello) throws IOException {

        if (hello.getHostname() == null) {
            log.error("Received HELLO packet with null hostname.");
            throw new ZicoException(ZicoPacket.ZICO_BAD_REQUEST, "Null hostname.");
        }

        HostStore store = getHost(hello.getHostname(), !enableSecurity);

        if (store == null) {
            throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, "Unauthorized.");
        }

        if (store.getAddr() == null || store.getAddr().length() == 0) {
            store.setAddr(socket.getInetAddress().getHostAddress());
            store.save();
        }

        if (enableSecurity) {
            if (store.getAddr() != null && !store.getAddr().equals(socket.getInetAddress().getHostAddress())) {
                throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, "Unauthorized.");
            }

            if (store.getPass() != null && !store.getPass().equals(hello.getAuth())) {
                throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, "Unauthorized.");
            }
        }

        return new ReceiverContext(store);
    }


    public List<HostStore> list(List<String> allowedHosts) {
        List<HostStore> result = new ArrayList<HostStore>();

        for (String name : new File(dataDir).list()) {
            if (new File(new File(dataDir, name), HostStore.HOST_PROPERTIES).exists()) {
                try {
                    if (allowedHosts == null || allowedHosts.contains(name)) {
                        result.add(getHost(name, false));
                    }
                } catch (Exception e) {
                    log.error("Error opening host. Consider repairing or removing this host." + name, e);
                }
            }
        }

        return result;
    }


    public synchronized void delete(String name) throws IOException {
        HostStore store = storesByName.get(name);
        if (store != null) {
            String rootPath = store.getRootPath();
            store.markFlag(HostInfo.DELETED);
            store.save();
            store.close();
            ZorkaUtil.rmrf(rootPath);
            storesByName.remove(name);
        }

    }


    public synchronized Map<Integer,String> getTids(String hostName) {
        Map<Integer,String> rslt = new HashMap<Integer,String>();

        if (hostName != null) {
            HostStore hs = getHost(hostName, false);
            if (hs != null) {
                rslt.putAll(hs.getTidMap());
            }
        } else {
            for (Map.Entry<String,HostStore> e : storesByName.entrySet()) {
                rslt.putAll(e.getValue().getTidMap());
            }
        }

        return rslt;
    }


    public void newHost(String name, String addr, String group, String desc, String pass, long maxsize) {

        if (getHost(name, false) != null) {
            throw new ZicoRuntimeException("Host named " + name + " already exist.");
        }

        HostStore host = getHost(name, true);
        host.setAddr(addr);
        host.setGroup(group);
        host.setComment(desc);
        host.setPass(pass);
        host.setMaxSize(maxsize);
        host.save();
    }


    public HostStore create(Class<? extends HostStore> clazz) {

        throw new ZicoRuntimeException("Not implemented.");
    }


    public HostStore find(Class<? extends HostStore> clazz, String id) {
        return getHost(id, false);
    }


    public void remove(HostStore host) {
        try {
            delete(host.getName());
        } catch (IOException e) {
            log.error("Cannot remove host", e);
        }
    }

}
