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


public class TraceInfoStatsQuery {

    @JsonProperty
    private String hostName;

    @JsonProperty
    private int traceId;

    @JsonProperty
    private Integer attrId;

    @JsonProperty
    private long startClock;

    @JsonProperty
    private long endClock;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getTraceId() {
        return traceId;
    }

    public void setTraceId(int traceId) {
        this.traceId = traceId;
    }

    public Integer getAttrId() {
        return attrId;
    }

    public void setAttrId(Integer attrId) {
        this.attrId = attrId;
    }

    public long getStartClock() {
        return startClock;
    }

    public void setStartClock(long startClock) {
        this.startClock = startClock;
    }

    public long getEndClock() {
        return endClock;
    }

    public void setEndClock(long endClock) {
        this.endClock = endClock;
    }

    @Override
    public String toString() {
        return "TraceInfoStatsQuery(host='" + hostName + "', traceId=" + traceId + ", attrId=" + attrId
                + ", startClock=" + startClock + ", endClock=" + endClock + ")";
    }
}
