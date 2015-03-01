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


import com.jitlogic.zico.core.eql.EqlException;
import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.core.eql.ast.EqlExpr;
import com.jitlogic.zico.core.search.EqlTraceRecordMatcher;
import com.jitlogic.zico.core.search.TraceRecordMatcher;
import com.jitlogic.zico.shared.data.TraceTemplateInfo;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class TraceTemplateManager {

    private final static Logger log = LoggerFactory.getLogger(TraceTemplateManager.class);

    private DB db;

    private NavigableMap<Integer,TraceTemplateInfo> templates;

    private volatile List<TraceTemplateInfo> orderedTemplates;

    private Map<Integer,EqlExpr> exprs = new ConcurrentHashMap<>();

    private ZicoConfig config;
    private DBFactory dbf;

    @Inject
    public TraceTemplateManager(ZicoConfig config, DBFactory dbf) {
        this.config = config;
        this.dbf = dbf;
        open();
    }


    public void open() {

        if (db != null) {
            return;
        }

        db = dbf.openDB(ZorkaUtil.path(config.getConfDir(), "templates.db"));
        templates = db.getTreeMap("TEMPLATES");

        File jsonFile = new File(config.getConfDir(), "templates.json");

        if (templates.size() == 0 && jsonFile.exists()) {
            log.info("Templates DB is empty but JSON dump was found. Importing...");
            Reader reader = null;
            try {
                reader = new FileReader(jsonFile);
                JSONObject json = new JSONObject(new JSONTokener(reader));
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    TraceTemplateInfo t = fromJSON(json.getJSONObject(names.getString(i)));
                    templates.put(t.getId(), t);
                }
                db.commit();
                log.info("Template DB import finished succesfully.");
            } catch (IOException e) {
                log.error("Cannot import user db from JSON data", e);
            } catch (JSONException e) {
                log.error("Cannot import user db from JSON data", e);
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (IOException e) { }
                }
            }
        }

        reorder();
    }


    public void close() {
        if (db != null) {
            dbf.closeDB(db);
            db = null;
            templates=  null;
            orderedTemplates = null;
        }
    }


    private void reorder() {
        List<TraceTemplateInfo> ttl = new ArrayList<>(templates.size());
        ttl.addAll(templates.values());
        Collections.sort(ttl, new Comparator<TraceTemplateInfo>() {
            @Override
            public int compare(TraceTemplateInfo o1, TraceTemplateInfo o2) {
                return o1.getOrder()-o2.getOrder();
            }
        });

        for (TraceTemplateInfo tti : ttl) {
            try {
                exprs.put(tti.getId(), Parser.expr(tti.getCondition()));
            } catch (EqlException e) {
                log.error("Cannot parse expression '" + tti.getCondition()
                        + "'. Please fix trace display templates configuration.", e);
            }
        }

        orderedTemplates = ttl;
    }


    public void export() {
        FileWriter writer = null;
        try {
            writer = new FileWriter(new File(config.getConfDir(), "templates.json"));
            JSONObject obj = new JSONObject();
            for (Map.Entry<Integer,TraceTemplateInfo> e : templates.entrySet()) {
                obj.put(e.getKey().toString(), toJSON(e.getValue()));
            }
            obj.write(writer);
        } catch (JSONException e) {
            log.error("Cannot export template DB", e);
        } catch (IOException e) {

        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException e) { }
            }
        }

    }


    public String templateDescription(SymbolRegistry symbolRegistry, String hostName, TraceRecord rec) {
        for (TraceTemplateInfo tti : orderedTemplates) {
            EqlExpr expr = exprs.get(tti.getId());
            TraceRecordMatcher matcher = new EqlTraceRecordMatcher(
                    symbolRegistry, expr, 0, rec.getTime(), hostName);
            if (expr != null && matcher.match(rec)) {
                return substitute(tti, symbolRegistry, hostName, rec);
            }
        }

        return genericTemplate(symbolRegistry, rec);
    }


    private String substitute(TraceTemplateInfo tti, SymbolRegistry symbolRegistry, String hostname, TraceRecord rec) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (rec.getAttrs() != null) {
            for (Map.Entry<Integer,Object> e : rec.getAttrs().entrySet()) {
                attrs.put(symbolRegistry.symbolName(e.getKey()), e.getValue());
            }
        }

        attrs.put("methodName", symbolRegistry.symbolName(rec.getMethodId()));
        attrs.put("className", symbolRegistry.symbolName(rec.getClassId()));
        attrs.put("hostName", hostname);

        return ObjectInspector.substitute(tti.getTemplate(), attrs);
    }


    public String genericTemplate(SymbolRegistry symbolRegistry, TraceRecord rec) {
        StringBuilder sdesc = new StringBuilder();

        if (rec.getMarker() != null) {
            sdesc.append(symbolRegistry.symbolName(rec.getMarker().getTraceId()));
        }

        if (rec.getAttrs() != null) {
            for (Map.Entry<Integer,Object> e : rec.getAttrs().entrySet()) {
                sdesc.append("|");
                String s = ""+e.getValue();
                sdesc.append(s.length() > 50 ? s.substring(0,50) : s);
            }
        }
        String s = sdesc.toString();
        return s.length() < 255 ? s : s.substring(0,255);
    }


    public List<TraceTemplateInfo> listTemplates() {
        List<TraceTemplateInfo> lst = new ArrayList<>(templates.size());
        lst.addAll(templates.values());
        return lst;
    }


    public synchronized int save(TraceTemplateInfo tti) {

        exprs.put(tti.getId(), Parser.expr(tti.getCondition()));

        if (tti.getId() == 0) {
            tti.setId(templates.size() > 0 ? templates.lastKey()+1 : 1);

            if (tti.getId() < 1000) {
                tti.setId(1000);
            }
        }


        templates.put(tti.getId(), tti);
        db.commit();

        reorder();

        return tti.getId();
    }


    public void remove(Integer tid) {
        templates.remove(tid);
        db.commit();
    }


    public TraceTemplateInfo create(Class<? extends TraceTemplateInfo> aClass) {
        return new TraceTemplateInfo();
    }


    public TraceTemplateInfo find(Class<? extends TraceTemplateInfo> aClass, Integer templateId) {
        return templates.get(templateId);
    }

    public static TraceTemplateInfo fromJSON(JSONObject obj) {
        TraceTemplateInfo tt = new TraceTemplateInfo();

        tt.setId(obj.getInt("id"));
        tt.setOrder(obj.getInt("order"));
        tt.setFlags(obj.getInt("flags"));

        tt.setCondition(obj.getString("condition"));
        tt.setTemplate(obj.getString("template"));

        return tt;
    }

    public static JSONObject toJSON(TraceTemplateInfo tt) {
        JSONObject obj = new JSONObject();

        obj.put("id", tt.getId());
        obj.put("order", tt.getOrder());
        obj.put("flags", tt.getFlags());
        obj.put("condition", tt.getCondition());
        obj.put("template", tt.getTemplate());

        return obj;
    }

}
