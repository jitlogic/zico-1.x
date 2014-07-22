package com.jitlogic.zico.core;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;

public class FileDBFactory implements DBFactory {

    @Override
    public DB openDB(String path) {
        return DBMaker.newFileDB(new File(path)).closeOnJvmShutdown().make();
    }

    @Override
    public void closeDB(DB db) {
        db.close();
    }

}
