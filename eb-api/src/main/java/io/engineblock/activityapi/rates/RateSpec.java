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

    public double burstRatio = 1.1D;

    /**
     * If true, report total scheduling delay from ideal schedule.
     */
    public boolean reportCoDelay = false;

    public RateSpec(double opsPerSec) {
        this(opsPerSec, 0.0d, false);
    }
    public RateSpec(double opsPerSec, double burstRatio) {
        this(opsPerSec, burstRatio, false);
    }
    public RateSpec(double opsPerSec, double burstRatio, boolean reportCoDelay) {
        this.opsPerSec = opsPerSec;
        this.burstRatio = burstRatio;
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
                burstRatio = Double.valueOf(specs[1]);
            case 1:
                opsPerSec = Unit.doubleCountFor(specs[0]).orElseThrow(() -> new RuntimeException("Unparsable:" + specs[0]));
                break;
            default:
                throw new RuntimeException("Rate specs must be either '<rate>' or '<rate>:<burstRatio>' as in 5000.0 or 5000.0:1.0");
        }
    }

    public String toString() {
        return "rate:" + opsPerSec
                + ", burst:" + burstRatio
                + ", report:" + reportCoDelay;
    }

    public RateSpec withOpsPerSecond(double rate) {
        return new RateSpec(rate,this.burstRatio,this.reportCoDelay);
    }

    public RateSpec withReportCoDelay(boolean reportCoDelay) {
        return new RateSpec(this.opsPerSec,this.burstRatio,reportCoDelay);
    }

    public RateSpec withBurstRatio(double burstRatio) {
        return new RateSpec(this.opsPerSec, burstRatio, this.reportCoDelay);
    }


    public long getCalculatedBurstNanos() {
        if (burstRatio==0.0) {
            return 0L;
        }
        if (burstRatio<1.0) {
            throw new RuntimeException("burst ratio must be either 0.0 (disabled), or be greater or equal to 1.0");
        }
        return (long) ((double)1_000_000_000L / (burstRatio*opsPerSec));
    }

    public long getCalculatedNanos() {
        return (long) ((double)1_000_000_000L / opsPerSec);
    }

    public double getRate() {
        return this.opsPerSec;
    }

    public double getBurstRatio() {
        return this.burstRatio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RateSpec rateSpec = (RateSpec) o;

        if (Double.compare(rateSpec.opsPerSec, opsPerSec) != 0) return false;
        if (Double.compare(rateSpec.burstRatio, burstRatio) != 0) return false;
        return reportCoDelay == rateSpec.reportCoDelay;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(opsPerSec);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(burstRatio);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (reportCoDelay ? 1 : 0);
        return result;
    }

    public boolean getReportCoDelay() {
        return reportCoDelay;
    }
}
