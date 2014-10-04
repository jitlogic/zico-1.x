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
package com.jitlogic.zico.util;


import com.jitlogic.zico.core.FileDBFactory;
import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.rds.RDSStore;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ZicoHostStoreCheckCommand implements ZicoCommand {

    private final static Logger log = LoggerFactory.getLogger(ZicoHostStoreCheckCommand.class);

    private ZicoConfig config;


    private QueuedHost queuedHost(String name) {
        long size = 0;

        File d = new File(ZorkaUtil.path(config.getDataDir(), name, "tdat"));

        if (d.isDirectory()) {
            for (String fname : d.list()) {
                File f = new File(d, fname);
                if (f.isFile() && RDSStore.RGZ_FILE.matcher(fname).matches()) {
                    size += f.length();
                }
            }
        }

        return new QueuedHost(name, size);
    }


    @Override
    public void run(String[] args) throws Exception {

        if (args.length < 2) {
            log.error("Host check command requires at least ZICO home dir and zero or more hostnames.");
            return;
        }

        Properties props = ZorkaConfig.defaultProperties(ZicoConfig.DEFAULT_CONF_PATH);
        props.setProperty("zico.home.dir", args[1]);
        config = new ZicoConfig(props);

        int nthreads = Runtime.getRuntime().availableProcessors() * 2;
        List<QueuedHost> hosts = new ArrayList<QueuedHost>(args.length);

        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                if (new File(config.getDataDir(), args[i]).isDirectory()) {
                    hosts.add(queuedHost(args[i]));
                } else {
                    log.error("Host " + args[i] + " does not exist. Skipping.");
                }
            }
        } else {
            for (String host : new File(config.getDataDir()).list()) {
                if (new File(config.getDataDir(), host).isDirectory()) {
                    hosts.add(queuedHost(host));
                }
            }
        }

        log.info("Index rebuild will be performed in " + nthreads + " threads.");

        if (hosts.size() == 0) {
            log.warn("No suitable hosts to be checked. Skipping.");
            return;
        }

        Collections.sort(hosts, new Comparator<QueuedHost>() {
            @Override
            public int compare(QueuedHost o1, QueuedHost o2) {
                return (int)(o2.getSize()-o1.getSize());
            }
        });

        log.info("Hosts to be processed (sorted by size): " + hosts);

        final CountDownLatch toBeProcessed = new CountDownLatch(hosts.size());
        final AtomicLong cpuTime = new AtomicLong(0);
        Executor executor = Executors.newFixedThreadPool(nthreads);

        for (final QueuedHost host : hosts) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        log.info("Starting host " + host);
                        long t1 = System.currentTimeMillis();
                        HostStore hostStore = new HostStore(new FileDBFactory(), config, null, host.getName());
                        hostStore.rebuildIndex();
                        long t = System.currentTimeMillis() - t1;
                        cpuTime.addAndGet(t);
                        log.info("Finished host " + host + " (t=" + t + "ms)");
                    } catch (Exception e) {
                        log.error("Error processing host " + host, e);
                    } finally {
                        toBeProcessed.countDown();
                    }
                }
            });
        }

        long t1 = System.currentTimeMillis();
        toBeProcessed.await();
        long t = System.currentTimeMillis()-t1;

        log.info("Migration finished: userTime=" + t + "ms, cpuTime=" + cpuTime.get() + "ms.");
    }

}
