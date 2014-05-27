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
package com.jitlogic.zico.shared.data;

import org.codehaus.jackson.annotate.JsonProperty;

public class TraceRecordSearchQuery {

    public final static int TXT_QUERY = 0;
    public final static int EQL_QUERY = 1;

    public final static int ERRORS_ONLY = 0x0001;
    public final static int METHODS_WITH_ATTRS = 0x0002;
    public final static int IGNORE_CASE = 0x0004;

    public final static int SEARCH_CLASSES = 0x0100;
    public final static int SEARCH_METHODS = 0x0200;
    public final static int SEARCH_ATTRS = 0x0400;
    public final static int SEARCH_EX_MSG = 0x0800;
    public final static int SEARCH_EX_STACK = 0x1000;
    public final static int SEARCH_SIGNATURE = 0x2000;

    public final static int SEARCH_ALL = SEARCH_CLASSES|SEARCH_METHODS|SEARCH_ATTRS|SEARCH_EX_MSG|SEARCH_EX_STACK|SEARCH_SIGNATURE;

    @JsonProperty
    String hostName;

    @JsonProperty
    long traceOffs;

    @JsonProperty
    long minTime;

    @JsonProperty
    String path;

    @JsonProperty
    int type;

    @JsonProperty
    int flags;

    @JsonProperty
    String searchExpr;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public long getTraceOffs() {
        return traceOffs;
    }

    public void setTraceOffs(long traceOffs) {
        this.traceOffs = traceOffs;
    }

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean hasFlag(int flag) {
        return 0 != (flag & flags);
    }

    public boolean emptyExpr() {
        return searchExpr == null || searchExpr.trim().length() == 0;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getSearchExpr() {
        return searchExpr;
    }

    public void setSearchExpr(String searchExpr) {
        this.searchExpr = searchExpr;
    }
}
