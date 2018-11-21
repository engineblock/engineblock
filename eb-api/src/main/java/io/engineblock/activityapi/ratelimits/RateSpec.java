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

package io.engineblock.activityapi.ratelimits;

import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.util.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateSpec {
    private final static Logger logger = LoggerFactory.getLogger(RateSpec.class);
    public static final double DEFAULT_RATE_OPS_S = 1.0D;
    public static final double DEFAULT_BURST_RATIO = 1.1D;

    /**
     * Target rate in Operations Per Second
     */
    public double opsPerSec = DEFAULT_RATE_OPS_S;
    public double burstRatio = DEFAULT_BURST_RATIO;

    public Type type= Type.hybrid; // Most users will expect limited bursting

    public Type getype() {
        return type;
    }

    public enum Type {
        average,    // no limit to bursting, but will aggressively track to overall average if allowed
        hybrid      // bursting is limited to a speed factor, and a background thread generates grants
    }

    public RateSpec(double opsPerSec, double burstRatio) {
        this(opsPerSec, burstRatio, Type.average);
    }
    public RateSpec(double opsPerSec, double burstRatio, Type type) {
        this.opsPerSec = opsPerSec;
        this.burstRatio = burstRatio;
        this.type = type;
    }

    public RateSpec(ParameterMap.NamedParameter tuple) {
        this(tuple.value);
        if (tuple.name.startsWith("co_")) {
            logger.warn("The co_ prefix on " + tuple.name + " is no longer needed. All rate limiters now provide standard coordinated omission metrics.");
        }
    }

    public RateSpec(String spec) {
        String[] specs = spec.split("[,:;]");
        switch (specs.length) {
            case 3:
                type = Type.valueOf(specs[2].toLowerCase());
                logger.debug("selected rate limiter type: " + type);
            case 2:
                burstRatio = Double.valueOf(specs[1]);
                if (burstRatio<1.0) {
                    throw new RuntimeException("burst ratios less than 1.0 are invalid.");
                }
            case 1:
                opsPerSec = Unit.doubleCountFor(specs[0]).orElseThrow(() -> new RuntimeException("Unparsable:" + specs[0]));
                break;
            default:
                throw new RuntimeException("Rate specs must be either '<rate>' or '<rate>:<burstRatio>' as in 5000.0 or 5000.0:1.0");
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        double ratePortion = Math.abs(opsPerSec - ((long)opsPerSec));
        String ratefmt = (ratePortion>0.001D) ? String.format("%,.3f",opsPerSec) : String.format("%,d",(long)opsPerSec);

        double br = burstRatio*opsPerSec;
        double burstPortion = Math.abs(br - ((long)br));
        String burstfmt = (burstPortion>0.001D) ? String.format("%,.3f",br) : String.format("%,d",(long)br);

        return String.format("rate=%s burstRatio=%.3f (%s SOPSS %s BOPSS)",ratefmt,burstRatio,ratefmt,burstfmt);
    }

    public RateSpec withOpsPerSecond(double rate) {
        return new RateSpec(rate,this.burstRatio);
    }


    public RateSpec withBurstRatio(double burstRatio) {
        return new RateSpec(this.opsPerSec, burstRatio);
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
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(opsPerSec);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(burstRatio);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
