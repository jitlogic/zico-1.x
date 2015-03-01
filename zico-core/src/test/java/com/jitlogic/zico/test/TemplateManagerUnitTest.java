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

import com.jitlogic.zico.core.TraceTemplateManager;
import com.jitlogic.zico.core.eql.EqlParseException;
import com.jitlogic.zico.core.services.TraceTemplateService;
import com.jitlogic.zico.shared.data.TraceTemplateInfo;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zico.test.support.ZicoTestUtil;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;
import static com.jitlogic.zico.test.support.ZicoTestUtil.*;

public class TemplateManagerUnitTest extends ZicoFixture {

    private TraceTemplateManager templateManager;
    private SymbolRegistry symbols = new SymbolRegistry();


    @Before
    public void acquireApplicationObjects() {
        templateManager = injector.getInstance(TraceTemplateManager.class);
        ZicoTestUtil.symbols = symbols;
    }


    @Test
    public void cleanup() {
        ZicoTestUtil.symbols = null;
    }


    private TraceTemplateInfo tti(int order, String condition, String templ) {
        TraceTemplateInfo tti = new TraceTemplateInfo();
        tti.setOrder(order);
        tti.setCondition(condition);
        tti.setTemplate(templ);
        return tti;
    }


    @Test
    public void testInsertNewTemplateRecord() throws Exception {
        TraceTemplateInfo t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");

        traceTemplateService.create(t1);

        List<TraceTemplateInfo> lst = traceTemplateService.list();
        assertEquals(1, lst.size());
        assertTrue("TEMPLATE_ID should be assigned by database and > 0", lst.get(0).getId() > 0);
    }


    @Test
    public void testInsertReopenAndReadTemplate() throws Exception {
        TraceTemplateInfo t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");

        traceTemplateService.create(t1);

        templateManager.close();
        templateManager.open();

        List<TraceTemplateInfo> lst = traceTemplateService.list();
        assertEquals(1, lst.size());
    }


    @Test
    public void testInsertAndModifyNewTemplate() {
        TraceTemplateInfo t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");

        traceTemplateService.create(t1);
        t1.setId(traceTemplateService.list().get(0).getId());

        t1.setTemplate("some.Class.findKey(${ARG0})");
        traceTemplateService.update(t1.getId(), t1);

        List<TraceTemplateInfo> lst = traceTemplateService.list();
        assertEquals(1, lst.size());
        assertEquals(t1.getId(), lst.get(0).getId());
    }


    @Test
    public void testAddRemoveTemplate() {
        TraceTemplateInfo t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");
        traceTemplateService.create(t1);
        t1.setId(traceTemplateService.list().get(0).getId());

        traceTemplateService.delete(t1.getId());
        assertEquals(0, traceTemplateService.list().size());
    }


    @Test
    public void testSearchForEmptyTraceIdMap() {
        assertThat(systemService.getTidMap(null)).hasSize(0);
    }


    @Test
    public void testExportImportTemplates() {
        TraceTemplateInfo t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");
        traceTemplateService.create(t1);
        int tid = traceTemplateService.list().get(0).getId();

        templateManager.export();
        templateManager.close();

        templateManager.open();

        TraceTemplateInfo t2 = traceTemplateService.get(tid);
        assertNotNull(t2);
        assertEquals(t1.getTemplate(), t2.getTemplate());
        assertEquals(t1.getOrder(), t2.getOrder());
        assertEquals(t1.getFlags(), t2.getFlags());
        assertEquals(t1.getCondition(), t2.getCondition());
    }


    @Test(expected = EqlParseException.class)
    public void testIfTemplateSaveVerifiesEqlExpr() throws Exception {
        TraceTemplateInfo t1 = tti(0, "METHOD='findKey", "findKey(${ARG0}");
        traceTemplateService.create(t1);
    }


    @Test
    public void testIfSavedTemplateGoesImmediatelyIntoEffect() throws Exception {
        TraceRecord rec = traceP("HTTP", "org.apache.catalina.StandardValve", "invoke", "()V", 1000, kv("URI", "http://xxx"));

        assertThat(templateManager.templateDescription(symbols, "websvr1", rec))
            .startsWith("HTTP|");

        traceTemplateService.create(tti(0, "URI != null", "GOT_IT"));

        assertThat(templateManager.templateDescription(symbols, "websvr1", rec))
                .startsWith("GOT_IT");

    }


    @Test
    public void testSelectTemplateByTemplateId() throws Exception {
        TraceRecord r1 = traceP("HTTP", "org.apache.catalina.StandardValve", "invoke", "()V", 1000, kv("URI", "http://xxx"));
        TraceRecord r2 = traceP("EJB", "org.jboss.ejb.Invoker", "invoke", "()V", 1000, kv("URI", "http://xxx"));
        traceTemplateService.create(tti(0, "trace = 'HTTP'", "GOT_IT"));
        assertEquals("GOT_IT", templateManager.templateDescription(symbols, "myhost", r1));
        assertNotEquals("GOT_IT", templateManager.templateDescription(symbols, "myhost", r2));
    }


    @Test
    public void testSelectTemplateByTemplateIdAndHost() throws Exception {
        TraceRecord rec = traceP("HTTP", "org.apache.catalina.StandardValve", "invoke", "()V", 1000, kv("URI", "http://xxx"));
        traceTemplateService.create(tti(0, "trace = 'HTTP' and host = 'myhost'", "GOT_IT"));
        assertEquals("GOT_IT", templateManager.templateDescription(symbols, "myhost", rec));
        assertNotEquals("GOT_IT", templateManager.templateDescription(symbols, "otherhost", rec));
    }
}
