package com.jitlogic.zico.core;

import org.mapdb.DB;

public interface DBFactory {

    public DB openDB(String path);

    public void closeDB(DB db);
}
