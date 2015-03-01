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

import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.shared.data.*;
import com.jitlogic.zico.core.rds.RAGZInputStream;
import com.jitlogic.zico.core.rds.RAGZSegment;
import com.jitlogic.zico.core.rds.RDSCleanupListener;
import com.jitlogic.zico.core.rds.RDSStore;
import com.jitlogic.zico.core.search.EqlTraceRecordMatcher;
import com.jitlogic.zico.core.search.FullTextTraceRecordMatcher;
import com.jitlogic.zico.core.search.TraceRecordMatcher;
import com.jitlogic.zorka.common.tracedata.FressianTraceFormat;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicException;
import com.jitlogic.zorka.common.tracedata.SymbolicStackElement;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.fressian.FressianReader;

import org.mapdb.BTreeMap;
import org.mapdb.DB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.regex.Pattern;

/**
 * Represents performance data store for a single agent.
 */
public class HostStore implements Closeable, RDSCleanupListener {

    private final static Logger log = LoggerFactory.getLogger(HostStore.class);

    public final static String HOST_PROPERTIES = "host.properties";

    private final static String DB_INFO_MAP = "INFOS";
    private final static String DB_TIDS_MAP = "TIDS";
    private final static String DB_ATTR_MAP = "ATTR";

    private PersistentSymbolRegistry symbolRegistry;

    private String rootPath;

    private TraceRecordStore traceDataStore;
    private TraceRecordStore traceIndexStore;

    private TraceTemplateManager templater;  // TODO move this out to service object

    private ZicoConfig config;


    private DB db;
    private BTreeMap<Long, TraceInfoRecord> infos;
    private BTreeMap<Integer,Set<Integer>> attrs;
    private Map<Integer, String> tids;

    private DBFactory dbf;

    private String name;

    private String addr = "";
    private String pass = "";
    private String group = "";

    private int flags;
    private long maxSize;
    private String comment = "";

    private static final long MAX_SEARCH_T1 = 2000000000L;
    private static final long MAX_SEARCH_T2 = 5000000000L;


    public HostStore(DBFactory dbf, ZicoConfig config, TraceTemplateManager templater, String name) {

        this.name = name;
        this.config = config;

        this.rootPath = ZorkaUtil.path(config.getDataDir(), ZicoUtil.safePath(name));

        this.dbf = dbf;

        File hostDir = new File(rootPath);

        if (!hostDir.exists()) {
            this.maxSize = config.kiloCfg("rds.max.size", 1024L * 1024L * 1024L);
            hostDir.mkdirs();
            save();
        } else {
            load();
        }

        this.templater = templater;

        if (!hasFlag(HostInfo.DISABLED)) {
            open();
        }
    }


    public synchronized void open() {
        try {
            load();
            if (symbolRegistry == null) {
                symbolRegistry = new PersistentSymbolRegistry(
                    new File(ZicoUtil.ensureDir(rootPath), "symbols.dat"));
            }

            if (traceDataStore == null) {
                traceDataStore = new TraceRecordStore(config, this, "tdat", 1, this);
            }

            if (traceIndexStore == null) {
                traceIndexStore = new TraceRecordStore(config, this, "tidx", 4);
            }

            db = dbf.openDB(ZorkaUtil.path(rootPath, "traces.db"));
            infos = db.getTreeMap(DB_INFO_MAP);
            tids = db.getTreeMap(DB_TIDS_MAP);
            attrs = db.getTreeMap(DB_ATTR_MAP);

        } catch (IOException e) {
            log.error("Cannot open host store " + name, e);
        }
    }


    @Override
    public synchronized void close() {

        if (symbolRegistry != null) {
            try {
                symbolRegistry.close();
            } catch (IOException e) {
                log.error("Cannot close symbol registry for " + name, e);
            }
            symbolRegistry = null;
        }

        if (traceDataStore != null) {
            try {
                traceDataStore.close();
            } catch (IOException e) {
                log.error("Cannot close trace data store for " + name, e);
            }
            traceDataStore = null;
        }

        if (traceIndexStore != null) {
            try {
                traceIndexStore.close();
            } catch (IOException e) {
                log.error("Cannot close trace index store for " + name, e);
            }
            traceIndexStore = null;
        }

        if (db != null) {
            dbf.closeDB(db);
            db = null;
            infos = null;
        }
    }


    public void export() {
        if (symbolRegistry != null) {
            symbolRegistry.export();
        }
    }


    public int countTraces() {
        BTreeMap<Long,TraceInfoRecord> infos = getInfos();

        if (infos == null) {
            throw new ZicoRuntimeException("Store " + getName() + " is closed.");
        }

        return infos.size();
    }


    public void processTraceRecord(TraceRecord rec) throws IOException {

        TraceRecordStore traceDataStore = getTraceDataStore();
        TraceRecordStore traceIndexStore = getTraceIndexStore();
        BTreeMap<Long,TraceInfoRecord> infos = getInfos();
        Map<Integer,String> tids = getTids();

        if (traceDataStore == null || traceIndexStore == null || infos == null || tids == null
                || hasFlag(HostInfo.DISABLED|HostInfo.DELETED)) {
            throw new ZicoRuntimeException("Store " + getName() + " is closed and cannot accept records.");
        }

        TraceRecordStore.ChunkInfo dchunk = traceDataStore.write(rec);

        List<TraceRecord> tmp = rec.getChildren();

        int numRecords = ZicoUtil.numRecords(rec);

        rec.setChildren(null);
        TraceRecordStore.ChunkInfo ichunk = traceIndexStore.write(rec);
        rec.setChildren(tmp);


        TraceInfoRecord tir = new TraceInfoRecord(rec,numRecords,
                dchunk.getOffset(), dchunk.getLength(),
                ichunk.getOffset(), ichunk.getLength(),
                rec.getAttrs() != null ? ZicoUtil.toIntArray(rec.getAttrs().keySet()) : null);

        checkAttrs(tir);

        infos.put(tir.getDataOffs(), tir);

        int traceId = tir.getTraceId();

        if (!tids.containsKey(traceId)) {
            tids.put(traceId, symbolRegistry.symbolName(traceId));
        }

    }


    public synchronized void rebuildIndex() throws IOException {

        int[] stats = new int[2];

        log.info("Start rebuildIndex() operation for host " + name);

        boolean enabled = isEnabled();

        flags |= HostInfo.CHK_IN_PROGRESS;

        if (enabled) {
            setEnabled(false);
        }

        // Remove old files (if any)
        File fTidx = new File(rootPath, "tidx");
        if (fTidx.exists()) {
            ZorkaUtil.rmrf(fTidx);
        }


        for (String s : new File(rootPath).list()) {
            if (s.startsWith("traces.db")) {
                new File(rootPath, s).delete();
            }
        }

        // Reopen and rebuild both db and idx
        open();

        File tdatDir = new File(rootPath, "tdat");
        List<String> fnames  = Arrays.asList(tdatDir.list());
        Collections.sort(fnames);

        for (String fname : fnames) {
            log.info("Processing file " + name + "/tdat/" + fname);
            if (RDSStore.RGZ_FILE.matcher(fname).matches()) {
                File f = new File(tdatDir, fname);
                try {
                    RAGZInputStream is = RAGZInputStream.fromFile(f);
                    long fileBasePos = Long.parseLong(fname.substring(0, 16), 16);
                    for (RAGZSegment seg : is.getSegments()) {
                        if (seg.getLogicalLen() > 0) {
                            byte[] buf = new byte[(int)seg.getLogicalLen()];
                            is.seek((int)seg.getLogicalPos());
                            is.read(buf);
                            try {
                                rebuildSegmentIndex(fileBasePos, seg, buf, stats);
                            } catch (Exception e) {
                                log.warn("Dropping rest of segment " + seg + " of " + name + "/tdat/"
                                        + fname + " due to error.", e);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing file " + f + " ; all traces saved in this file will be dropped.", e);
                }
            }
        }

        db.commit();
        close();

        flags &= ~HostInfo.CHK_IN_PROGRESS;

        if (enabled) {
            setEnabled(enabled);
        }

        save();

        log.info("Operation rebuildIndex() for host " + name + " finished. "
            + stats[0] + " traces imported, " + stats[1] + " traces dropped.");
    }


    private void rebuildSegmentIndex(long fileBasePos, RAGZSegment seg, byte[] buf, int[] stats) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        Object obj;
        long basePos = seg.getLogicalPos() + fileBasePos, lastPos = basePos;
        while (bis.available() > 0) {
            try {
                FressianReader reader = new FressianReader(bis, FressianTraceFormat.READ_LOOKUP);
                obj = reader.readObject();
                long dataLen = basePos + buf.length - bis.available() - lastPos;
                if (obj instanceof TraceRecord) {
                    TraceRecord tr = (TraceRecord)obj;
                    int numRecords = ZicoUtil.numRecords(tr);
                    tr.setChildren(null);
                    TraceRecordStore.ChunkInfo idxChunk = traceIndexStore.write(tr);
                    TraceInfoRecord tir = new TraceInfoRecord(tr, numRecords,
                            lastPos, (int)dataLen, idxChunk.getOffset(), idxChunk.getLength(),
                            tr.getAttrs() != null ? ZicoUtil.toIntArray(tr.getAttrs().keySet()) : null);
                    checkAttrs(tir);
                    infos.put(lastPos, tir);
                    int traceId = tir.getTraceId();
                    if (!tids.containsKey(traceId)) {
                        tids.put(traceId, symbolRegistry.symbolName(traceId));
                    }
                    stats[0]++;
                }
                lastPos += dataLen;
            } catch (EOFException e) {
                // This is normal.
            }
        }
    }

    private void checkAttrs(TraceInfoRecord tir) {
        Set<Integer> ta = attrs.get(tir.getTraceId());
        if (ta == null || tir.getAttrs() != null && !ZicoUtil.containsAll(ta, tir.getAttrs())) {
            Set<Integer> nta = new HashSet<>();
            if (ta != null) {
                nta.addAll(ta);
            }
            if (tir.getAttrs() != null) {
                for (int i : tir.getAttrs()) {
                    nta.add(i);
                }
            }
            attrs.put(tir.getTraceId(), nta);
        }
    }


    public synchronized void commit() {
        if (db != null) {
            db.commit();
        }
    }


    private void checkEnabled() {
        if (hasFlag(HostInfo.DISABLED)) {
            throw new ZicoRuntimeException("Host " + name
                    + " is disabled. Bring it back online before issuing operation.");
        }
    }

    private synchronized BTreeMap<Long,TraceInfoRecord> getInfos() {
        return infos;
    }

    public Map<Integer,Set<Integer>> scanTraceAttrIds() {

        Map<Integer,Set<Integer>> rslt = new HashMap<>();

        for (Long key  = infos.lastKey(); key != null; key = infos.lowerKey(key)) {
            TraceInfoRecord tir = infos.get(key);

            int tid = tir.getTraceId();

            if (!rslt.containsKey(tid)) {
                rslt.put(tid, new HashSet<Integer>());
            }

            if (tir.getAttrs() != null) {
                Set<Integer> ids = rslt.get(tid);
                for (int i : tir.getAttrs()) {
                    ids.add(i);
                }
            }
        }

        return rslt;
    }

    // To be used later
    public Map<Integer,Set<Integer>> getTraceAttrIds() {
        Map<Integer,Set<Integer>> rslt = new HashMap<>();
        rslt.putAll(attrs);
        return rslt;
    }

    public Map<Integer,Map<String,Integer>> getTraceAttrNames() {
        Map<Integer,Map<String,Integer>> rslt = new HashMap<>();

        for (Map.Entry<Integer,Set<Integer>> e : getTraceAttrIds().entrySet()) {
            if (!rslt.containsKey(e.getKey())) {
                Map<String,Integer> m = new HashMap<>();
                rslt.put(e.getKey(), m);
                for (Integer id : e.getValue()) {
                    m.put(symbolRegistry.symbolName(id), id);
                }
            }
        }

        return rslt;
    }

    public List<SymbolInfo> getTraceAttrNames(int traceId) {
        List<SymbolInfo> lst = new ArrayList<>(attrs.size());

        if (attrs.containsKey(traceId)) {
            for (Integer id : attrs.get(traceId)) {
                String name = symbolRegistry.symbolName(id);
                if (name != null) {
                    lst.add(new SymbolInfo(id, name));
                }
            }
        }

        Collections.sort(lst, new Comparator<SymbolInfo>() {
            @Override
            public int compare(SymbolInfo o1, SymbolInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return lst;
    }

    // TODO factor out stats() functionality into separate class
    public List<TraceInfoStatsResult> stats(TraceInfoStatsQuery query) throws IOException {

        Map<String,TraceInfoStatsResult> acc = new HashMap<>();

        TraceRecordStore traceIndexStore = getTraceIndexStore();

        for (Long key = infos.lastKey(); key != null; key = infos.lowerKey(key)) {
            TraceInfoRecord tir = infos.get(key);

            if (tir.getClock() < query.getStartClock()) {
                break;
            }

            if (tir.getClock() > query.getEndClock()) {
                continue;
            }

            if (tir.getTraceId() == query.getTraceId() && tir.getAttrs() != null
                    && ZicoUtil.contains(tir.getAttrs(), query.getAttrId())) {

                TraceRecord idxtr = traceIndexStore.read(tir.getIndexChunk());

                Object av = idxtr.getAttr(query.getAttrId());

                if (av != null) {
                    String attr = av.toString();
                    TraceInfoStatsResult rslt;
                    if (!acc.containsKey(attr)) {
                        rslt = new TraceInfoStatsResult();
                        rslt.setAttr(attr);
                        rslt.setMinTime(Long.MAX_VALUE);
                        rslt.setMaxTime(Long.MIN_VALUE);
                        acc.put(attr, rslt);
                    } else {
                        rslt = acc.get(attr);
                    }

                    rslt.setCalls(rslt.getCalls()+1);
                    if (idxtr.getMarker() != null && idxtr.getMarker().hasFlag(TraceMarker.ERROR_MARK)) {
                        rslt.setErrors(rslt.getErrors()+1);
                    }

                    rslt.setMinTime(Math.min(rslt.getMinTime(), idxtr.getTime()));
                    rslt.setMaxTime(Math.max(rslt.getMaxTime(), idxtr.getTime()));
                    rslt.setSumTime(rslt.getSumTime() + idxtr.getTime());
                }
            }
        }

        List<TraceInfoStatsResult> rslt = new ArrayList<>(acc.size()+1);
        rslt.addAll(acc.values());

        Collections.sort(rslt, new Comparator<TraceInfoStatsResult>() {
            @Override
            public int compare(TraceInfoStatsResult o1, TraceInfoStatsResult o2) {
                return (int)((o2.getSumTime()-o1.getSumTime())/1000000L);
            }
        });

        return rslt.size() > query.getMaxResults() ? rslt.subList(0, query.getMaxResults()) : rslt;
    }

    // TODO factor out search() functionality into separate class
    public TraceInfoSearchResult search(TraceInfoSearchQuery query) throws IOException {

        SymbolRegistry symbolRegistry = getSymbolRegistry();
        BTreeMap<Long,TraceInfoRecord> infos = getInfos();
        TraceRecordStore traceDataStore = getTraceDataStore();
        TraceRecordStore traceIndexStore = getTraceIndexStore();

        if (symbolRegistry == null || infos == null || traceDataStore == null || traceIndexStore == null) {
            throw new ZicoRuntimeException("Host store " + getName() + " is closed.");
        }

        List<TraceInfo> lst = new ArrayList<TraceInfo>(query.getLimit());

        TraceInfoSearchResult result = new TraceInfoSearchResult();
        result.setSeq(query.getSeq());
        result.setResults(lst);

        TraceRecordMatcher matcher = null;

        int traceId = query.getTraceName() != null ? symbolRegistry.symbolId(query.getTraceName()) : 0;

        if (query.getSearchExpr() != null) {
            if (query.hasFlag(TraceInfoSearchQuery.EQL_QUERY)) {
                matcher = new EqlTraceRecordMatcher(symbolRegistry,
                        Parser.expr(query.getSearchExpr()),
                        0, 0, getName());
            } else if (query.getSearchExpr().length() > 0 && query.getSearchExpr().startsWith("~")) {
                matcher = new FullTextTraceRecordMatcher(symbolRegistry,
                        TraceRecordSearchQuery.SEARCH_ALL, Pattern.compile(query.getSearchExpr().substring(1)));
            } else {
                matcher = new FullTextTraceRecordMatcher(symbolRegistry,
                        TraceRecordSearchQuery.SEARCH_ALL, query.getSearchExpr());
            }
        }

        // TODO implement query execution time limit

        int searchFlags = query.getFlags();

        boolean asc = 0 == (searchFlags & TraceInfoSearchQuery.ORDER_DESC);

        Long initialKey = asc
                ? infos.higherKey(query.getOffset() != 0 ? query.getOffset() : Long.MIN_VALUE)
                : infos.lowerKey(query.getOffset() != 0 ? query.getOffset() : Long.MAX_VALUE);

        long tstart = System.nanoTime();

        for (Long key = initialKey; key != null; key = asc ? infos.higherKey(key) : infos.lowerKey(key)) {

            long t = System.nanoTime()-tstart;

            if ((lst.size() >= query.getLimit()) || (t > MAX_SEARCH_T1 && lst.size() > 0) || (t > MAX_SEARCH_T2)) {
                result.markFlag(TraceInfoSearchResult.MORE_RESULTS);
                return result;
            }

            TraceInfoRecord tir = infos.get(key);

            result.setLastOffs(key);

            if (query.hasFlag(TraceInfoSearchQuery.ERRORS_ONLY) && 0 == (tir.getTflags() & TraceMarker.ERROR_MARK)) {
                continue;
            }

            if (query.getStartDate() != 0 && tir.getClock() < query.getStartDate()) {
                continue;
            }

            if (query.getEndDate() != 0 && tir.getClock() > query.getEndDate()) {
                continue;
            }

            if (traceId != 0 && tir.getTraceId() != traceId) {
                continue;
            }

            if (tir.getDuration() < query.getMinMethodTime()) {
                continue;
            }

            TraceRecord idxtr = (query.hasFlag(TraceInfoSearchQuery.DEEP_SEARCH) && matcher != null)
                    ? traceDataStore.read(tir.getDataChunk())
                    : traceIndexStore.read(tir.getIndexChunk());

            if (idxtr != null) {
                if (matcher instanceof EqlTraceRecordMatcher) {
                    ((EqlTraceRecordMatcher) matcher).setTotalTime(tir.getDuration());
                }
                if (matcher == null || recursiveMatch(matcher, idxtr)) {
                    lst.add(toTraceInfo(tir, idxtr));
                }
            }

        }

        return result;
    }


    private boolean recursiveMatch(TraceRecordMatcher matcher, TraceRecord tr) {
        if (matcher.match(tr)) {
            return true;
        }

        if (tr.numChildren() > 0) {
            for (TraceRecord c : tr.getChildren()) {
                if (matcher.match(c)) {
                    return true;
                }
            }
        }

        return false;
    }


    public TraceInfoRecord getInfoRecord(long offs) {
        checkEnabled();
        return infos.get(offs);
    }


    private TraceInfo toTraceInfo(TraceInfoRecord itr, TraceRecord tr) throws IOException {

        if (tr == null) {
            tr = traceIndexStore.read(itr.getIndexChunk());
        }

        TraceInfo ti = new TraceInfo();
        ti.setHostName(name);
        ti.setDataOffs(itr.getDataOffs());
        ti.setTraceId(itr.getTraceId());
        ti.setTraceType(symbolRegistry.symbolName(itr.getTraceId()));
        ti.setMethodFlags(itr.getRflags());
        ti.setTraceFlags(itr.getTflags());
        ti.setClassId(tr.getClassId());
        ti.setMethodId(tr.getMethodId());
        ti.setSignatureId(tr.getSignatureId());
        ti.setStatus(tr.getException() != null                 // TODO get rid of this field
                || tr.hasFlag(TraceRecord.EXCEPTION_PASS)
                || tr.getMarker().hasFlag(TraceMarker.ERROR_MARK) ? 1 : 0);
        ti.setRecords(itr.getRecords());
        ti.setDataLen(itr.getDataLen());
        ti.setCalls(itr.getCalls());
        ti.setErrors(itr.getErrors());
        ti.setExecutionTime(itr.getDuration());
        ti.setClock(tr.getMarker().getClock());

        if (tr != null && tr.getAttrs() != null) {
            List<KeyValuePair> keyvals = new ArrayList<KeyValuePair>(tr.getAttrs().size());
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                keyvals.add(new KeyValuePair(symbolRegistry.symbolName(e.getKey()), "" + e.getValue()));
            }
            ti.setAttributes(ZicoUtil.sortKeyVals(keyvals));
        } else {
            ti.setAttributes(Collections.EMPTY_LIST);
        }

        if (tr.getException() != null) {
            SymbolicExceptionInfo sei = new SymbolicExceptionInfo();
            SymbolicException sex = (SymbolicException) tr.getException();
            sei.setExClass(symbolRegistry.symbolName(sex.getClassId()));
            sei.setMessage(sex.getMessage());
            List<String> lst = new ArrayList<String>();

            for (SymbolicStackElement sel : sex.getStackTrace()) {
                lst.add(symbolRegistry.symbolName(sel.getClassId()) + ":" + sel.getLineNum());
                if (lst.size() > 10) {
                    break;
                }
            }

            sei.setStackTrace(lst);

            ti.setExceptionInfo(sei);
        }

        ti.setDescription(templater.templateDescription(symbolRegistry, name, tr));

        return ti;
    }


    public synchronized Map<Integer,String> getTids() {
        return tids;
    }


    public Map<Integer, String> getTidMap() {
        Map<Integer,String> tids = getTids();

        if (tids == null) {
            throw new ZicoRuntimeException("Host store " + getName() + " is closed");
        }

        Map<Integer,String> tidMap = new HashMap<Integer, String>();

        for (Integer tid : tids.keySet()) {
            tidMap.put(tid, symbolRegistry.symbolName(tid));
        }

        return tidMap;
    }


    public String toString() {
        return "HostStore(" + getName() + ")";
    }


    public synchronized void load() {
        File f = new File(rootPath, HOST_PROPERTIES);
        Properties props = new Properties();
        if (f.exists() && f.canRead()) {
            InputStream is = null;
            try {
                is = new FileInputStream(f);
                props.load(is);
            } catch (IOException e) {
                log.error("Cannot read " + f, e);
            } finally {
                if (is != null) {
                    try { is.close(); } catch (IOException e) { }
                }
            }
            this.addr = props.getProperty("addr", "127.0.0.1");
            this.pass = props.getProperty("pass", "");
            this.flags = Integer.parseInt(props.getProperty("flags", "0"));
            this.maxSize = Long.parseLong(props.getProperty("size",
                    "" + config.kiloCfg("rds.max.size", 1024 * 1024 * 1024L)));
            this.group = props.getProperty("group", "");
            this.comment = props.getProperty("comment", "");
        }
    }


    public synchronized void save() {

        Properties props = new Properties();
        props.setProperty("addr", addr);
        props.setProperty("pass", pass);
        props.setProperty("flags", "" + flags);
        props.setProperty("size", "" + maxSize);
        props.setProperty("group", group);
        props.setProperty("comment", comment);

        File f = new File(rootPath, HOST_PROPERTIES);

        OutputStream os = null;
        try {
            os = new FileOutputStream(f);
            props.store(os, "ZICO Host Descriptor");
        } catch (IOException e) {
            log.error("Cannot write " + f, e);
        } finally {
            if (os != null) {
                try { os.close(); } catch (IOException e) { }
            }
        }

        if (traceDataStore != null && maxSize != traceDataStore.getRds().getMaxSize()) {
            traceDataStore.getRds().setMaxSize(maxSize);
            try {
                traceDataStore.getRds().cleanup();
            } catch (IOException e) {
                log.error("Cannot perform cleanup for host store " + name, e);
            }
        }
    }


    public synchronized TraceRecordStore getTraceDataStore() {
        return traceDataStore;
    }


    public synchronized TraceRecordStore getTraceIndexStore() {
        return traceIndexStore;
    }


    public synchronized SymbolRegistry getSymbolRegistry() {
        return symbolRegistry;
    }


    public synchronized String getRootPath() {
        return rootPath;
    }

    private DB getDb() {
        return db;
    }

    @Override
    public void onChunkRemoved(RDSStore origin, Long start, Long length) {

        TraceRecordStore traceIndexStore = getTraceIndexStore();
        BTreeMap<Long,TraceInfoRecord> infos = getInfos();
        DB db = getDb();

        long end = start + length - 1;
        long t0 = System.currentTimeMillis();

        log.info("Removing old index entries for host " + name);
        long idxOffs = -1;
        int count = 0;
        if (infos != null && db != null) {
            Map.Entry<Long, TraceInfoRecord> nextEntry = infos.ceilingEntry(end);
            idxOffs = nextEntry != null ? nextEntry.getValue().getIndexOffs()-1 : -1;
            ConcurrentNavigableMap<Long, TraceInfoRecord> rmitems = infos.headMap(end);
            count = rmitems.size();
            rmitems.clear();
            db.commit();
        }

        if (idxOffs > 0 && traceIndexStore != null) {
            try {
                log.info("Cleaning up index RDS for host " + name + " (idxOffs=" + idxOffs + ")");
                traceIndexStore.getRds().cleanup(idxOffs);
            } catch (IOException e) {
                log.error("Problem cleaning up index store", e);
            }
        }
        long t = System.currentTimeMillis() - t0;
        log.info("Removing " + count + " entries from " + name + " took " + t + "ms.");
    }


    public synchronized String getName() {
        return name;
    }


    public synchronized String getAddr() {
        return addr;
    }


    public synchronized void setAddr(String addr) {
        this.addr = addr;
    }


    public String getPass() {
        return pass;
    }


    public void setPass(String pass) {
        this.pass = pass;
    }


    public synchronized int getFlags() {
        return flags;
    }


    public synchronized boolean hasFlag(int flag) {
        return 0 != (this.flags & flag);
    }

    public synchronized void markFlag(int flag) {
        flags |= flag;
    }


    public synchronized long getMaxSize() {
        return maxSize;
    }


    public synchronized void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        if (traceDataStore != null) {
            traceDataStore.getRds().setMaxSize(maxSize);
        }
        if (traceIndexStore != null) {
            traceIndexStore.getRds().setMaxSize(maxSize);
        }
    }

    public synchronized String getGroup() {
        return group;
    }

    public synchronized void setGroup(String group) {
        this.group = group;
    }

    public synchronized String getComment() {
        return comment;
    }


    public synchronized void setComment(String comment) {
        this.comment = comment;
    }


    public synchronized boolean isEnabled() {
        return !hasFlag(HostInfo.DISABLED);
    }


    public synchronized void setEnabled(boolean enabled) {

        if (enabled) {
            log.info("Bringing host " + name + " online.");
            open();
            flags &= ~HostInfo.DISABLED;
        } else {
            log.info("Taking host " + name + " offline.");
            close();
            flags |= HostInfo.DISABLED;
        }
    }

}

