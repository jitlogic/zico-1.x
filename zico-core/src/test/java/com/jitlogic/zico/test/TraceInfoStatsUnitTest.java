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

package com.jitlogic.zico.test;

import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.shared.data.TraceInfoStatsQuery;
import com.jitlogic.zico.shared.data.TraceInfoStatsResult;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zico.test.support.ZicoTestUtil;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.jitlogic.zico.test.support.ZicoTestUtil.*;
import static org.junit.Assert.*;

public class TraceInfoStatsUnitTest extends ZicoFixture {

    HostStore hs;

    @Before
    public void setUpHostAndSyms() throws Exception {
        hs = hostStoreManager.getHost("test", true);
        ZicoTestUtil.symbols = hs.getSymbolRegistry();
    }


    @Test
    public void testGetTraceAttrIds() throws Exception {
        hs.processTraceRecord(trace(kv("A", "1"), kv("B", "2")));
        hs.processTraceRecord(trace(kv("C", "3"), kv("A", "1")));
        hs.commit();

        Map<Integer,Set<Integer>> rslt = hs.getTraceAttrIds();

        assertEquals(ZorkaUtil.<Integer,Set<Integer>>map(
            sym("MY_TRACE"), ZorkaUtil.set(sym("A"),sym("B"),sym("C"))),
            rslt);
    }

    @Test
    public void testGetTraceAttrNames() throws Exception {
        hs.processTraceRecord(trace(kv("A", "1"), kv("B", "2")));
        hs.processTraceRecord(trace(kv("C", "3"), kv("A", "1")));
        hs.commit();

        Map<Integer,Map<String,Integer>> rslt = hs.getTraceAttrNames();

        assertEquals(ZorkaUtil.<Integer,Map<Integer,String>>map(
            sym("MY_TRACE"),
            ZorkaUtil.map("A", sym("A"), "B", sym("B"), "C",sym("C"))),
            rslt);
    }

    private void ptr(String attr, long clock, long time) throws Exception {
        TraceRecord tr = trace(kv("SQL", attr));
        tr.getMarker().setClock(clock);
        tr.setTime(time);
        hs.processTraceRecord(tr);
    }


    private TraceInfoStatsResult tisr(int calls, int errors, int sumTime, int minTime, int maxTime, String attr) {
        TraceInfoStatsResult rslt = new TraceInfoStatsResult();

        rslt.setCalls(calls);
        rslt.setErrors(errors);
        rslt.setSumTime(sumTime);
        rslt.setMaxTime(maxTime);
        rslt.setMinTime(minTime);
        rslt.setAttr(attr);

        return rslt;
    }


    @Test
    public void testGetTraceStats() throws Exception {
        ptr("SELECT 1", 50, 50);
        ptr("SELECT 1", 150, 20);
        ptr("SELECT 1", 250, 80);
        ptr("SELECT 1", 350, 50);
        ptr("SELECT 2", 200, 50);

        TraceInfoStatsQuery q = new TraceInfoStatsQuery();
        q.setHostName("test");
        q.setTraceId(sym("MY_TRACE"));
        q.setAttrId(sym("SQL"));
        q.setStartClock(100);
        q.setEndClock(300);

        Set<TraceInfoStatsResult> rslt = new HashSet<>();
        rslt.addAll(hs.stats(q));

        assertEquals(ZorkaUtil.<TraceInfoStatsResult>set(
                        tisr(2, 0, 100, 20, 80, "SELECT 1"),
                        tisr(1, 0, 50, 50, 50, "SELECT 2")
                ),
                rslt);
    }
}
