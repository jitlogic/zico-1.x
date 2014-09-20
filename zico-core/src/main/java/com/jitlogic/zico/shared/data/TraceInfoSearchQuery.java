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

public class TraceInfoSearchQuery {

    public static final int ORDER_DESC  = 0x0001;
    public static final int DEEP_SEARCH = 0x0002;
    public static final int ERRORS_ONLY = 0x0004;
    public static final int EQL_QUERY   = 0x0008;

    @JsonProperty
    private int seq;

    @JsonProperty
    private String hostName;

    @JsonProperty
    private int flags;

    @JsonProperty
    private long offset;

    @JsonProperty
    private int limit;

    @JsonProperty
    private String traceName;

    @JsonProperty
    private long minMethodTime;

    @JsonProperty
    private String searchExpr;

    @JsonProperty
    private long startDate;

    @JsonProperty
    private long endDate;

    public int getSeq() {
        return seq;
    }


    public void setSeq(int seq) {
        this.seq = seq;
    }


    public String getHostName() {
        return hostName;
    }


    public void setHostName(String hostName) {
        this.hostName = hostName;
    }


    public int getFlags() {
        return flags;
    }


    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean hasFlag(int flag) {
        return 0 != (this.flags & flag);
    }


    public long getOffset() {
        return offset;
    }


    public void setOffset(long offset) {
        this.offset = offset;
    }


    public int getLimit() {
        return limit;
    }


    public void setLimit(int limit) {
        this.limit = limit;
    }


    public String getTraceName() {
        return traceName;
    }


    public void setTraceName(String traceName) {
        this.traceName = traceName;
    }


    public long getMinMethodTime() {
        return minMethodTime;
    }


    public void setMinMethodTime(long minMethodTime) {
        this.minMethodTime = minMethodTime;
    }


    public String getSearchExpr() {
        return searchExpr;
    }


    public void setSearchExpr(String searchExpr) {
        this.searchExpr = searchExpr;
    }


    public long getStartDate() {
        return startDate;
    }


    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }


    public long getEndDate() {
        return endDate;
    }


    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }
}
