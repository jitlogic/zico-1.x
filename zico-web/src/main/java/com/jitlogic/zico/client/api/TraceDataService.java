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

import com.jitlogic.zico.shared.data.*;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import javax.ws.rs.*;
import java.util.List;
import java.util.Map;

public interface TraceDataService extends RestService {

    @POST
    @Path("traces/search")
    void search(TraceInfoSearchQuery query, MethodCallback<TraceInfoSearchResult> result);

    @GET @Path("traces/attrs/{host}/{id}")
    void attrNames(@PathParam("host") String host, @PathParam("id") int id, MethodCallback<List<SymbolInfo>> cb);

    @POST @Path("traces/stats")
    void statTraces(TraceInfoStatsQuery query, MethodCallback<List<TraceInfoStatsResult>> cb);

    @POST @Path("traces/records/rank")
    void rankRecords(TraceRecordRankQuery query, MethodCallback<List<MethodRankInfo>> cb);

    @POST @Path("traces/records/get")
    void getRecord(TraceRecordListQuery query, MethodCallback<TraceRecordInfo> cb);

    @POST @Path("traces/records/list")
    void listRecords(TraceRecordListQuery query, MethodCallback<List<TraceRecordInfo>> cb);

    @POST @Path("traces/records/search")
    void searchRecords(TraceRecordSearchQuery query, MethodCallback<TraceRecordSearchResult> cb);
}
