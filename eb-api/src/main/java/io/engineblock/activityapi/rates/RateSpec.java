/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package io.engineblock.activityapi.rates;

import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.util.Unit;

public class RateSpec {
    /**
     * Target rate in Operations Per Second
     */
    public double opsPerSec = 1.0D;

    /**
     * Rate Scheduling Strictness
     *  0.0D - average only
     *  0.5D - Soak half of unused schedule time away each clock update
     *  1.0 - Soak all unused schedule time away each clock update
     *  1.5 - Allow for 150% burst rate until average rate is met
     */
    public double strictness = 0.0D;

    /**
     * If true, report total scheduling delay from ideal schedule.
     */
    public boolean reportCoDelay = false;

    public RateSpec(double opsPerSec) {
        this(opsPerSec, 0.0d, false);
    }
    public RateSpec(double opsPerSec, double strictness) {
        this(opsPerSec, strictness, false);
    }
    public RateSpec(double opsPerSec, double strictness, boolean reportCoDelay) {
        this.opsPerSec = opsPerSec;
        this.strictness = strictness;
        this.reportCoDelay = reportCoDelay;
    }

    public RateSpec(ParameterMap.NamedParameter tuple) {
        this(tuple.value);
        if (tuple.name.startsWith("co_")) {
            reportCoDelay =true;
        }
    }

    public RateSpec(String spec) {
        String[] specs = spec.split("[,:;]");
        switch (specs.length) {
            case 3:
                reportCoDelay = (specs[2].toLowerCase().matches("co|true|report"));
            case 2:
                strictness = Double.valueOf(specs[1]);
            case 1:
                opsPerSec = Unit.countFor(specs[0]).orElseThrow(() -> new RuntimeException("Unparsable:" + specs[0]));
                break;
            default:
                throw new RuntimeException("Rate specs must be either '<rate>' or '<rate>:<strictness>' as in 5000.0 or 5000.0:1.0");
        }
    }

    public String toString() {
        return "opsPerSec:" + opsPerSec
                + ", strictness:" + strictness
                + ", reportCoDelay:" + reportCoDelay;
    }

    public RateSpec withOpsPerSecond(double rate) {
        this.opsPerSec = rate;
        return this;
    }

    public RateSpec withReportCoDelay(boolean reportCoDelay) {
        this.reportCoDelay = reportCoDelay;
        return this;
    }

    public RateSpec withStrictness(double strictness) {
        this.strictness = strictness;
        return this;
    }


}
