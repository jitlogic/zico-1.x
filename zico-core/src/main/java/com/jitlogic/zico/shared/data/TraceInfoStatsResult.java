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

import java.util.Map;

public class TraceInfoStatsResult {

    @JsonProperty
    private String attr;

    @JsonProperty
    private int calls;

    @JsonProperty
    private int errors;

    @JsonProperty
    private long sumTime;

    @JsonProperty
    private long maxTime;

    @JsonProperty
    private long minTime;

    public String getAttr() {
        return attr;
    }

    public void setAttr(String attr) {
        this.attr = attr;
    }

    public int getCalls() {
        return calls;
    }

    public void setCalls(int calls) {
        this.calls = calls;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public long getSumTime() {
        return sumTime;
    }

    public void setSumTime(long sumTime) {
        this.sumTime = sumTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TraceInfoStatsResult) {
            TraceInfoStatsResult r = (TraceInfoStatsResult)obj;
            return r.calls == calls && r.errors == errors
                && r.sumTime == sumTime && r.maxTime == maxTime && r.minTime == minTime
                && ((r.attr == null && attr == null) || (attr != null && attr.equals(r.attr)));
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return (int)(errors + 11 * calls + 17 * sumTime + 31 * maxTime + 41 * minTime);
    }


    @Override
    public String toString() {
        return "StatsResult(calls=" + calls + ", errors=" + errors + ", sumTime=" + sumTime
            + ", minTime=" + minTime + ", maxTime=" + maxTime + ", attr=" + attr + ")";
    }
}
