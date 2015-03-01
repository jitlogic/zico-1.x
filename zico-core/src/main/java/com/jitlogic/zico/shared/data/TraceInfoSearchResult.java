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
package com.jitlogic.zico.shared.data;


import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class TraceInfoSearchResult {

    public static final int MORE_RESULTS = 0x0001;

    @JsonProperty
    int seq;

    @JsonProperty
    int flags;

    @JsonProperty
    long lastOffs;

    @JsonProperty
    List<TraceInfo> results;

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void markFlag(int flag) {
        this.flags |= flag;
    }

    public long getLastOffs() {
        return lastOffs;
    }

    public void setLastOffs(long lastOffs) {
        this.lastOffs = lastOffs;
    }

    public List<TraceInfo> getResults() {
        return results;
    }

    public void setResults(List<TraceInfo> results) {
        this.results = results;
    }
}
