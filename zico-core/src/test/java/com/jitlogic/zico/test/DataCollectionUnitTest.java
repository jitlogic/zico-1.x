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
package com.jitlogic.zico.test;


import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.ZicoRuntimeException;
import com.jitlogic.zico.shared.data.TraceInfo;
import com.jitlogic.zico.shared.data.TraceInfoSearchQuery;
import com.jitlogic.zico.shared.data.TraceInfoSearchResult;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zorka.common.tracedata.FressianTraceWriter;
import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.zico.ZicoTraceOutput;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jitlogic.zico.test.support.ZicoTestUtil.*;
import static org.fest.reflect.core.Reflection.field;
import static org.fest.reflect.core.Reflection.method;
import static org.junit.Assert.*;


public class DataCollectionUnitTest extends ZicoFixture {

    private ZicoTraceOutput output;

    @Before
    public void setUpOutputAndCollector() throws Exception {

        symbols = new SymbolRegistry();
        metrics = new MetricsRegistry();

        output = new ZicoTraceOutput(
                new FressianTraceWriter(symbols, metrics),
                "127.0.0.1", 9640, "test", "aaa", 64, 8 * 1024 * 1024, 1, 250, 8, 30000);
    }


    private void submit(TraceRecord... recs) throws Exception {
        method("open").in(output).invoke();
        for (TraceRecord rec : recs) {
            output.submit(rec);
        }
        method("runCycle").in(output).invoke();
    }


    private int countTraces(String hostName) {
        HostStore store = hostStoreManager.getHost(hostName, false);

        assertNotNull("store should be existing", store);

        return store.countTraces();
    }


    @Test(timeout = 1000)
    public void testCollectSingleTraceRecord() throws Exception {
        submit(trace());

        assertEquals("One trace should be noticed.", 1, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectTwoTraceRecords() throws Exception {
        submit(trace());
        assertEquals("One trace should be noticed.", 1, countTraces("test"));
        submit(trace());
        assertEquals("Two traces should be noticed.", 2, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectThreeTraceRecordsInOneGo() throws Exception {
        submit(trace(), trace(), trace());
        assertEquals("Two traces should be noticed.", 3, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectThreeRecordsWithLimitPerPacket() throws Exception {
        field("packetSize").ofType(long.class).in(output).set(200L);
        submit(trace(), trace(), trace());
        assertEquals("Two traces should be noticed.", 2, countTraces("test"));
        submit();
        assertEquals("Two traces should be noticed.", 3, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectBrokenTraceCausingNPE() throws Exception {
        TraceRecord rec = trace();
        rec.setFlags(0);


        submit(rec);

        assertEquals("Trace will not reach store.", 0, countTraces("test"));

        submit(trace());
        assertEquals("TraceOutput should reconnect and send properly.", 1, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testSubmitCloseReopenReadTrace() throws Exception {
        submit(trace());
        assertEquals("One trace should be noticed.", 1, countTraces("test"));

        hostStoreManager.close();
        assertEquals("One trace should be still there.", 1, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testSubmitAndSearchSingleRecordWithoutCriteria() throws Exception {
        submit(trace(kv("SQL", "select count(1) from HOSTS")));
        TraceInfoSearchResult result = traceDataService.searchTraces(tiq("test", 0, null, 0L, null));

        assertEquals(1, result.getResults().size());

        TraceInfo ti = result.getResults().get(0);
        assertEquals(1, ti.getAttributes().size());
        assertEquals("SQL", ti.getAttributes().get(0).getKey());
        assertTrue(ti.getDescription().startsWith("MY_TRACE"));
    }


    @Test(timeout = 1000)
    public void testSubmitMoreRecordsAndSearchWithSimpleCriteria() throws Exception {
        submit(trace(kv("SQL", "select count(*) from HOSTS")), trace(kv("SQL", "select count(1) from TRACES")));
        TraceInfoSearchResult result = traceDataService.searchTraces(tiq("test", 0, null, 0L, "TRACES"));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitTwoRecordsAndFilterByTime() throws Exception {
        TraceRecord t1 = trace(); t1.setTime(500);
        TraceRecord t2 = trace(); t2.setTime(1500);
        submit(t1, t2);
        TraceInfoSearchResult result = traceDataService.searchTraces(tiq("test", 0, null, 1000L, null));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitTwoRecordsAndFilterByTraceType() throws Exception {
        submit(traceP("HTTP", rClass(), rMethod(), rSignature(), 100),
                traceP("EJB", rClass(), rMethod(), rSignature(), 200));
        TraceInfoSearchResult result = traceDataService.searchTraces(tiq("test", 0, "EJB", 1000L, null));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndFilterByMethodErrorMarker() throws Exception {
        TraceRecord t1 = trace(); t1.getMarker().setFlags(TraceMarker.ERROR_MARK);
        submit(t1, trace());
        TraceInfoSearchResult result = traceDataService.searchTraces(
                tiq("test", TraceInfoSearchQuery.ERRORS_ONLY, null, 1000L, null));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndFilterByMethodErrorObject() throws Exception {
        TraceRecord t1 = trace(); t1.setException(boo());
        submit(t1, trace());
        TraceInfoSearchQuery query = tiq("test", TraceInfoSearchQuery.ERRORS_ONLY, null, 1000L, null);
        TraceInfoSearchResult result = traceDataService.searchTraces(query);
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndFilterUsingEqlQuery() throws Exception {
        submit(trace(), trace(kv("SQL", "select 1")));
        TraceInfoSearchResult result = traceDataService.searchTraces(
                tiq("test", TraceInfoSearchQuery.EQL_QUERY, null, 1000L, "SQL != null"));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndFilterUsingDeepSearch() throws Exception {
        submit(trace(), trace(trace(kv("SQL", "select 1"))));
        TraceInfoSearchResult result = traceDataService.searchTraces(
                tiq("test", TraceInfoSearchQuery.DEEP_SEARCH, null, 0, "SQL"));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndListInDescendingOrder() throws Exception {
        submit(trace(), trace());
        TraceInfoSearchQuery query = tiq("test", TraceInfoSearchQuery.ORDER_DESC, null, 0, null);
        TraceInfoSearchResult result = traceDataService.searchTraces(query);
        List<TraceInfo> lst = result.getResults();
        assertEquals(2, lst.size());
        assertTrue("records should be in descending order", lst.get(0).getDataOffs() > lst.get(1).getDataOffs());
    }


    @Test
    public void testSubmitAndPageResults() throws Exception {
        submit(trace(), trace(), trace(), trace());
        TraceInfoSearchQuery query = tiq("test", 0, null, 0, null);
        query.setLimit(2);
        TraceInfoSearchResult rslt1 = traceDataService.searchTraces(query);
        assertEquals(2, rslt1.getResults().size());
        query.setOffset(rslt1.getLastOffs()); query.setLimit(100);
        TraceInfoSearchResult rslt2 = traceDataService.searchTraces(query);
        assertEquals(2, rslt2.getResults().size());
        assertTrue(rslt1.getResults().get(1).getDataOffs() < rslt2.getResults().get(0).getDataOffs());
    }


    @Test
    public void testSubmitAndPageResultsDesc() throws Exception {
        submit(trace(), trace(), trace(), trace());
        TraceInfoSearchQuery query = tiq("test", TraceInfoSearchQuery.ORDER_DESC, null, 0, null);
        query.setLimit(2);
        TraceInfoSearchResult rslt1 = traceDataService.searchTraces(query);
        assertEquals(2, rslt1.getResults().size());
        query.setOffset(rslt1.getLastOffs()); query.setLimit(100);
        TraceInfoSearchResult rslt2 = traceDataService.searchTraces(query);
        assertEquals(2, rslt2.getResults().size());
        assertTrue(rslt1.getResults().get(1).getDataOffs() > rslt2.getResults().get(0).getDataOffs());
    }


    @Test(expected = ZicoRuntimeException.class)
    public void testSubmitAndThenSearchNonExistentHost() throws Exception {
        submit(trace());
        traceDataService.searchTraces(tiq("nooone", 0, null, 0, null));
    }


    @Test(expected = ZicoRuntimeException.class)
    public void testTrySubmitToOfflineStore() throws Exception {
        HostStore testHost = hostStoreManager.getHost("test", true);
        testHost.setEnabled(false);
        testHost.search(tiq("test", 0, null, 0, null));
    }


    @Test
    public void testStoreSomethingAndRebuildIndex() throws Exception {
        submit(trace(trace(),trace()), trace(trace(),trace()));
        TraceInfoSearchResult rslt1 = traceDataService.searchTraces(tiq("test", 0, null, 0, null));
        HostStore host = hostStoreManager.getHost("test", false);

        host.rebuildIndex();
        host.setEnabled(true);

        TraceInfoSearchResult rslt2 = traceDataService.searchTraces(tiq("test", 0, null, 0, null));

        List<TraceInfo> lst1 = rslt1.getResults(), lst2 = rslt2.getResults();
        assertEquals(lst1.size(), lst2.size());

        for (int i = 0; i < lst1.size(); i++) {
            assertEquals(lst1.get(i).getDataOffs(), lst2.get(i).getDataOffs());
            assertEquals(lst1.get(i).getDataLen(), lst2.get(i).getDataLen());
            assertEquals("Should count properly number of records", 3, lst2.get(i).getRecords());
        }
    }


    @Test
    public void testStoreTwoSegmentsAndRebuildIndex() throws Exception {
        submit(trace(trace(),trace()));
        HostStore host = hostStoreManager.getHost("test", false);
        host.close(); host.open();
        submit(trace());

        TraceInfoSearchResult rslt1 = traceDataService.searchTraces(tiq("test", 0, null, 0, null));

        host.rebuildIndex();

        TraceInfoSearchResult rslt2 = traceDataService.searchTraces(tiq("test", 0, null, 0, null));

        List<TraceInfo> lst1 = rslt1.getResults(), lst2 = rslt2.getResults();
        assertEquals(lst1.size(), lst2.size());

        for (int i = 0; i < lst1.size(); i++) {
            assertEquals(lst1.get(i).getDataOffs(), lst2.get(i).getDataOffs());
            assertEquals(lst1.get(i).getDataLen(), lst2.get(i).getDataLen());
        }
    }

    @Test
    public void testStoreInTwoFilesTwoSegmentsPerFileAndRebuildIndex() throws Exception {
        submit(trace());
        HostStore host = hostStoreManager.getHost("test", false);
        host.close(); host.open();
        submit(trace());
        host.getTraceDataStore().getRds().rotate();
        submit(trace());
        host.close(); host.open();
        submit(trace());

        TraceInfoSearchResult rslt1 = traceDataService.searchTraces(tiq("test", 0, null, 0, null));

        host.rebuildIndex();

        TraceInfoSearchResult rslt2 = traceDataService.searchTraces(tiq("test", 0, null, 0, null));

        List<TraceInfo> lst1 = rslt1.getResults(), lst2 = rslt2.getResults();
        assertEquals(lst1.size(), lst2.size());

        for (int i = 0; i < lst1.size(); i++) {
            assertEquals(lst1.get(i).getDataOffs(), lst2.get(i).getDataOffs());
            assertEquals(lst1.get(i).getDataLen(), lst2.get(i).getDataLen());
        }

    }

    @Test
    public void testIfDataFilesAndIndexesAreRotatedProperly() throws Exception {
        config.setCfg("rds.file.size", 200);
        config.setCfg("rds.seg.size", 100);
        config.setCfg("rds.max.size", 500);

        for (int i = 0; i < 10; i++) {
            submit(trace(rec(kv("OJA", "AAA")),rec()));
        }

        List<String> dFiles = Arrays.asList(new File(ZorkaUtil.path(config.getDataDir(), "test", "tdat")).list());
        Collections.sort(dFiles);

        List<String> iFiles = Arrays.asList(new File(ZorkaUtil.path(config.getDataDir(), "test", "tidx")).list());
        Collections.sort(iFiles);

        TraceInfoSearchResult rslt = traceDataService.searchTraces(tiq("test", 0, null, 0, null));
        long firstOffs = rslt.getResults().get(0).getDataOffs();

        assertTrue("first record found in index should exist in data store",
                firstOffs >= Long.parseLong(dFiles.get(0).substring(0,16),16));

        HostStore host = hostStoreManager.getHost("test", false);

        assertTrue("tidx datastore files should be truncated properly",
                host.getInfoRecord(firstOffs).getIndexOffs() <= Long.parseLong(iFiles.get(1).substring(0,16),16));
    }



    // TODO test: search in host without access - should fail in a controlled way

    // TODO automatic index cleanup after main store truncation

    // TODO import hosts
}
