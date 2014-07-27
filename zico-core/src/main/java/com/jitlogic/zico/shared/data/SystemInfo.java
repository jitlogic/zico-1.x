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

public class SystemInfo {

    @JsonProperty
    long totalHeapMem;

    @JsonProperty
    long usedHeapMem;

    @JsonProperty
    long totalNonHeapMem;

    @JsonProperty
    long usedNonHeapMem;

    @JsonProperty
    long uptime;


    public long getTotalHeapMem() {
        return totalHeapMem;
    }

    public void setTotalHeapMem(long totalHeapMem) {
        this.totalHeapMem = totalHeapMem;
    }

    public long getUsedHeapMem() {
        return usedHeapMem;
    }

    public void setUsedHeapMem(long usedHeapMem) {
        this.usedHeapMem = usedHeapMem;
    }

    public long getTotalNonHeapMem() {
        return totalNonHeapMem;
    }

    public void setTotalNonHeapMem(long totalNonHeapMem) {
        this.totalNonHeapMem = totalNonHeapMem;
    }

    public long getUsedNonHeapMem() {
        return usedNonHeapMem;
    }

    public void setUsedNonHeapMem(long usedNonHeapMem) {
        this.usedNonHeapMem = usedNonHeapMem;
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }
}
