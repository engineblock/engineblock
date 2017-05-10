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
package io.engineblock.activityimpl.input;

import com.codahale.metrics.Gauge;
import com.google.common.util.concurrent.RateLimiter;
import io.engineblock.activityapi.ActivityDefObserver;
import io.engineblock.activityapi.Input;
import io.engineblock.activityapi.RateLimiterProvider;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.ActivityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>This input will provide threadsafe access to a sequence of long values.</p>
 * <p>If the targetrate parameter is set on the activity def, then the rate of
 * grants will also be throttled to that rate.</p>
 * <p>Changes to the cycles or the targetrate will affect the provided inputs.
 * If the min or max cycle is changed, then these are re-applied first to the
 * max cycle and then to the min cycle. If the min cycle is changed, then the
 * next cycle value is set to the assigned min value. Otherwise, the cycle
 * will continue as usual till it reaches the max value. The ability to reset
 * the input while running by applying a new set of parameters makes it possible
 * to re-trigger a sequence of inputs during a test.</p>
 * <p>This input, and Inputs in general do not actively prevent usage of values
 * after the max value. They simply expose it to callers. It is up to the
 * caller to check the value to determine when the input is deemed "used up."</p>
 */
public class TargetRateInput implements Input, ActivityDefObserver, RateLimiterProvider {
    private final static Logger logger = LoggerFactory.getLogger(TargetRateInput.class);

    private final AtomicLong cycleValue = new AtomicLong(0L);
    private final AtomicLong min = new AtomicLong(0L);
    private final AtomicLong max = new AtomicLong(Long.MAX_VALUE);
    // TODO: Consider a similar approach to this: http://blog.nirav.name/2015/02/a-simple-rate-limiter-using-javas.html
    private RateLimiter rateLimiter;
    private ActivityDef activityDef;

    public TargetRateInput(ActivityDef activityDef) {
        this.activityDef = activityDef;
        onActivityDefUpdate(activityDef);
    }


    public TargetRateInput setNextValue(long newValue) {
        if (newValue < min.get() || newValue > max.get()) {
            throw new RuntimeException(
                    "new value (" + newValue + ") must be within min..max range: [" + min + ".." + max + "]"
            );
        }
        cycleValue.set(newValue);
        return this;
    }

    @Override
    public long getAsLong() {
        if (rateLimiter != null) {
            rateLimiter.acquire();
        }
        return cycleValue.getAndIncrement();
    }

    @Override
    public long getInterval(long stride) {
        if (rateLimiter != null) {
            rateLimiter.acquire();
        }
        return cycleValue.getAndAdd(stride);
    }

    @Override
    public AtomicLong getMin() {
        return min;
    }

    @Override
    public AtomicLong getMax() {
        return max;
    }

    @Override
    public long getCurrent() {
        return cycleValue.get();
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {

        long startCycle = activityDef.getStartCycle();
        long endCycle = activityDef.getEndCycle();
        if (startCycle > endCycle) {
            throw new InvalidParameterException("min (" + min + ") must be less than or equal to max (" + max + ")");
        }

        if (max.get() != endCycle) {
            max.set(endCycle);
        }

        if (min.get() != startCycle) {
            min.set(startCycle);
            setNextValue(min.get());
        }

        updateRateLimiter(activityDef);

    }

    private void updateRateLimiter(ActivityDef activityDef) {
        double rate = activityDef.getParams().getOptionalDoubleUnitCount("targetrate").orElse(Double.NaN);
        if (!Double.isNaN(rate)) {
            if (rateLimiter==null) {
                rateLimiter = RateLimiter.create(rate);
            } else {
                rateLimiter.setRate(rate);
            }

            Gauge<Double> rateGauge = new Gauge<Double>() {
                @Override
                public Double getValue() {
                    return rateLimiter.getRate();
                }
            };

            ActivityMetrics.gauge(activityDef,"targetrate",rateGauge);

            logger.info("targetrate was set to:" + rate);
        }
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

}
