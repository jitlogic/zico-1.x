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
package com.jitlogic.zico.core;


import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

import java.io.Serializable;
import java.util.Set;

public class TraceInfoRecord implements Serializable {

    private long dataOffs;

    private int dataLen;

    private long indexOffs;

    private int indexLen;

    private int traceId;

    private long clock;

    private long duration;

    private long calls;

    private long errors;

    private long records;

    private int tflags;

    private int rflags;

    private int[] attrs;

    public TraceInfoRecord() {
    }


    public TraceInfoRecord(TraceRecord tr, int numRecords, long dataOffs, int dataLen, long indexOffs, int indexLen, int[] attrs) {
        this.dataOffs = dataOffs;
        this.dataLen = dataLen;
        this.indexOffs = indexOffs;
        this.indexLen = indexLen;
        this.traceId = tr.getMarker().getTraceId();
        this.clock = tr.getClock();
        this.duration = tr.getTime();
        this.calls = tr.getCalls();
        this.errors = tr.getErrors();
        this.records = numRecords;
        this.tflags = tr.getMarker().getFlags();
        this.rflags = tr.getFlags();
        this.attrs = attrs;

        if (tr.getException() != null || tr.hasFlag(TraceRecord.EXCEPTION_PASS)) {
            this.tflags |= TraceMarker.ERROR_MARK;
        }

    }

    public long getDataOffs() {
        return dataOffs;
    }

    public int getDataLen() {
        return dataLen;
    }

    public long getIndexOffs() {
        return indexOffs;
    }

    public int getIndexLen() {
        return indexLen;
    }

    public int getTraceId() {
        return traceId;
    }

    public long getClock() {
        return clock;
    }

    public long getDuration() {
        return duration;
    }

    public long getCalls() {
        return calls;
    }

    public long getErrors() {
        return errors;
    }

    public long getRecords() {
        return records;
    }

    public int getTflags() {
        return tflags;
    }

    public int getRflags() {
        return rflags;
    }

    public int[] getAttrs() {
        return attrs;
    }

    @Override
    public int hashCode() {
        return (int)dataOffs;
    }

    public TraceRecordStore.ChunkInfo getIndexChunk() {
        return new TraceRecordStore.ChunkInfo(indexOffs, indexLen);
    }

    public TraceRecordStore.ChunkInfo getDataChunk() {
        return new TraceRecordStore.ChunkInfo(dataOffs, dataLen);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() == TraceInfoRecord.class) {
            TraceInfoRecord ti = (TraceInfoRecord)obj;
            return dataOffs == ti.dataOffs;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "TraceInfoRecord(" + indexOffs + "," + dataOffs + ")";
    }

}
