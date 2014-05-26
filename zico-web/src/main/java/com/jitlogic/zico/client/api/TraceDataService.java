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

import com.jitlogic.zico.shared.data.*;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

public interface TraceDataService extends RestService {

    @POST
    @Path("traces/search")
    void search(TraceInfoSearchQuery query, MethodCallback<TraceInfoSearchResult> result);

    @POST @Path("traces/records/rank")
    void rankRecords(TraceRecordRankQuery query, MethodCallback<List<MethodRankInfo>> cb);

    @POST @Path("traces/records/get")
    void getRecord(TraceRecordListQuery query, MethodCallback<TraceRecordInfo> cb);

    @POST @Path("traces/records/list")
    void listRecords(TraceRecordListQuery query, MethodCallback<List<TraceRecordInfo>> cb);

    @POST @Path("traces/records/search")
    void searchRecords(TraceRecordSearchQuery query, MethodCallback<TraceRecordSearchResult> cb);
}
