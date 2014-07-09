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


import com.jitlogic.zico.core.*;
import com.jitlogic.zico.core.eql.EqlException;
import com.jitlogic.zico.core.eql.EqlParseException;
import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.core.TraceInfoRecord;
import com.jitlogic.zico.core.search.EqlTraceRecordMatcher;
import com.jitlogic.zico.core.search.FullTextTraceRecordMatcher;
import com.jitlogic.zico.core.search.TraceRecordMatcher;
import com.jitlogic.zico.shared.data.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Singleton
@Path("/traces")
public class TraceDataService {

    private final static Logger log = LoggerFactory.getLogger(TraceDataService.class);

    @Inject
    private HostStoreManager storeManager;

    @Inject
    private UserManager userManager;


    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public TraceInfoSearchResult searchTraces(TraceInfoSearchQuery query) {
        userManager.checkHostAccess(query.getHostName());
        try {
            HostStore host = storeManager.getHost(query.getHostName(), false);
            if (host == null) {
                throw new ZicoRuntimeException("Unknown host: " + query.getHostName());
            }
            return host.search(query);
        } catch (EqlParseException e) {
            log.error("Error while parsing eql query '" + query.getSearchExpr() + "'\n" + e, e);
            throw new ZicoRuntimeException(e.toString() + " [query '" + query.getSearchExpr() + "']", e);
        } catch (EqlException e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException(e.toString() + " [query '" + query.getSearchExpr() + "']", e);
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error while searching '" + query.getSearchExpr() + "': " + e.getMessage(), e);
        }
    }


    @POST
    @Path("/records/rank")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<MethodRankInfo> traceMethodRank(TraceRecordRankQuery q) {
        userManager.checkHostAccess(q.getHostname());
        try {
            HostStore host = storeManager.getHost(q.getHostname(), false);
            if (host != null) {
                TraceInfoRecord info = host.getInfoRecord(q.getTraceOffs());
                if (info != null && host.getTraceDataStore() != null) {
                    return host.getTraceDataStore().methodRank(info, q.getOrderBy(), q.getOrderDesc());
                }
            }
            return Collections.EMPTY_LIST;
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error while calling MethodRank", e);
        }
    }

    @POST
    @Path("/records/get")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public TraceRecordInfo getRecord(TraceRecordListQuery q) {
        userManager.checkHostAccess(q.getHostName());
        try {
            HostStore host = storeManager.getHost(q.getHostName(), false);
            if (host != null) {
                TraceInfoRecord info = host.getInfoRecord(q.getTraceOffs());
                if (info != null) {
                    return ZicoUtil.packTraceRecord(host.getSymbolRegistry(),
                            host.getTraceDataStore().getTraceRecord(info, q.getPath(), q.getMinTime()), q.getPath(), null);
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error calling getRecord()", e);
        }
    }


    @POST
    @Path("/records/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<TraceRecordInfo> listRecords(TraceRecordListQuery q) {
        userManager.checkHostAccess(q.getHostName());
        try {
            HostStore host = storeManager.getHost(q.getHostName(), false);
            if (host != null) {
                TraceInfoRecord info = host.getInfoRecord(q.getTraceOffs());
                TraceRecordStore ctx = host.getTraceDataStore();
                if (info != null && ctx != null) {
                    TraceRecord tr = ctx.getTraceRecord(info, q.getPath(), q.getMinTime());

                    List<TraceRecordInfo> lst = new ArrayList<TraceRecordInfo>();

                    if (q.getPath() != null) {
                        packRecords(host.getSymbolRegistry(), q.getPath(), ctx, tr, lst, q.isRecursive());
                    } else {
                        lst.add(ZicoUtil.packTraceRecord(host.getSymbolRegistry(), tr, "", 250));
                        if (q.isRecursive()) {
                            packRecords(host.getSymbolRegistry(), "", ctx, tr, lst, q.isRecursive());
                        }
                    }
                    return lst;
                }
            }

            return Collections.EMPTY_LIST;
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error calling listRecords()", e);
        }
    }


    @POST @Path("/records/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public TraceRecordSearchResult searchRecords(TraceRecordSearchQuery q) {
        userManager.checkHostAccess(q.getHostName());
        try {
            HostStore host = storeManager.getHost(q.getHostName(), false);
            if (host != null) {
                TraceInfoRecord info = host.getInfoRecord(q.getTraceOffs());
                TraceRecordStore ctx = host.getTraceDataStore();
                if (ctx != null && info != null) {
                    TraceRecord tr = ctx.getTraceRecord(info, q.getPath(), q.getMinTime());
                    TraceRecordSearchResult result = new TraceRecordSearchResult();
                    result.setResult(new ArrayList<TraceRecordInfo>());
                    result.setMinTime(Long.MAX_VALUE);
                    result.setMaxTime(Long.MIN_VALUE);

                    TraceRecordMatcher matcher;
                    String se = q.getSearchExpr();
                    switch (q.getType()) {
                        case TraceRecordSearchQuery.TXT_QUERY:
                            if (se != null && se.startsWith("~")) {
                                int rflag = 0 != (q.getFlags() & TraceRecordSearchQuery.IGNORE_CASE) ? Pattern.CASE_INSENSITIVE : 0;
                                Pattern regex = Pattern.compile(se.substring(1, se.length()), rflag);
                                matcher = new FullTextTraceRecordMatcher(host.getSymbolRegistry(), q.getFlags(), regex);
                            } else {
                                matcher = new FullTextTraceRecordMatcher(host.getSymbolRegistry(), q.getFlags(), se);
                            }
                            break;
                        case TraceRecordSearchQuery.EQL_QUERY:
                            matcher = new EqlTraceRecordMatcher(host.getSymbolRegistry(), Parser.expr(se),
                                    q.getFlags(), tr.getTime(), host.getName());
                            break;
                        default:
                            throw new ZicoRuntimeException("Illegal search expression type: " + q.getType());
                    }
                    ctx.searchRecords(tr, q.getPath(), matcher, result, tr.getTime(), false);

                    if (result.getMinTime() == Long.MAX_VALUE) {
                        result.setMinTime(0);
                    }

                    if (result.getMaxTime() == Long.MIN_VALUE) {
                        result.setMaxTime(0);
                    }

                    return result;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error calling listRecords()", e);
        }

    }

    private void packRecords(SymbolRegistry symbolRegistry, String path, TraceRecordStore ctx, TraceRecord tr,
                             List<TraceRecordInfo> lst, boolean recursive) {
        for (int i = 0; i < tr.numChildren(); i++) {
            TraceRecord child = tr.getChild(i);
            String childPath = path.length() > 0 ? (path + "/" + i) : "" + i;
            lst.add(ZicoUtil.packTraceRecord(symbolRegistry, child, childPath, 250));
            if (recursive && child.numChildren() > 0) {
                packRecords(symbolRegistry, childPath, ctx, child, lst, recursive);
            }
        }
    }

}
