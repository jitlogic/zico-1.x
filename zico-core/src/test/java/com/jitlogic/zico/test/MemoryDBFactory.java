package com.jitlogic.zico.test;

import com.jitlogic.zico.core.DBFactory;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.HashMap;
import java.util.Map;

public class MemoryDBFactory implements DBFactory {

    private Map<String,DB> dbs = new HashMap<>();

    @Override
    public DB openDB(String path) {
        if (!dbs.containsKey(path)) {
            dbs.put(path, DBMaker.newMemoryDB().make());
        }
        return dbs.get(path);
    }

    @Override
    public void closeDB(DB db) {

    }

}
