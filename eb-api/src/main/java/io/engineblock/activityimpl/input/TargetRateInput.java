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
import io.engineblock.activityapi.core.ActivityDefObserver;
import io.engineblock.activityapi.cyclelog.buffers.cycles.CycleSegment;
import io.engineblock.activityapi.input.Input;
import io.engineblock.activityapi.input.RateLimiterProvider;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.rates.CoreRateLimiter;
import io.engineblock.rates.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>TODO: This documentation is out of date as of 2.0.0
 * <p>This input will provide threadsafe access to a sequence of long values.</p>
 * <p>If the targetrate parameter is set on the activity def, then the rate of
 * grants will also be throttled to that rate.</p>
 * <p>Changes to the cycles or the targetrate will affect the provided inputs.
 * If the min or max cycle is changed, then these are re-applied first to the
 * max cycle and then to the min cycle. If the min cycle is changed, then the
 * next cycle value is set to the assigned min value. Otherwise, the cycle
 * will continue as usual till it reaches the max value. The ability to start
 * the input while running by applying a new set of parameters makes it possible
 * to re-trigger a sequence of inputs during a test.</p>
 * <p>This input, and Inputs in general do not actively prevent usage of values
 * after the max value. They simply expose it to callers. It is up to the
 * caller to check the value to determine when the input is deemed "used up."</p>
 */
public class TargetRateInput implements Input, ActivityDefObserver, RateLimiterProvider, ProgressCapable {
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

    @Override
    public CycleSegment getInputSegment(int stride) {
        if (rateLimiter!=null) {
            rateLimiter.acquire();
        }
        while (true) {
            long current = this.cycleValue.get();
            long next = current + stride;
            if (next >max.get()) {
                return null;
            }
            if (cycleValue.compareAndSet(current,next)) {
                return new InputInterval.Segment(current,next);
            }
        }
    }

    @Override
    public double getProgress()
    {
        return (double) (cycleValue.get() - min.get());
    }

    @Override
    public double getTotal()
    {
        return (double) (max.get() - min.get());
    }

    @Override
    public String toString() {
        return "TargetRateInput{" +
                "cycleValue=" + cycleValue +
                ", min=" + min +
                ", max=" + max +
                ", rateLimiter=" + rateLimiter +
                ", activity=" + activityDef.getAlias() +
                '}';
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
            cycleValue.set(min.get());
        }

        updateRateLimiter(activityDef);

    }

    private void updateRateLimiter(ActivityDef activityDef) {
        activityDef.getParams().getOptionalDoubleUnitCount("targetrate").ifPresent(
        rate -> {
            if (rateLimiter==null) {
                rateLimiter = new CoreRateLimiter(rate);
            } else {
                double oldRate = rateLimiter.getRate();
                rateLimiter.setRate(rate);
                double newRate = rateLimiter.getRate();
                // RateLimiter turns 30000.0 into 29999.999999999996 so we check if the internal value changed here
                if(oldRate == newRate)
                {
                    return;
                }
            }
            Gauge<Double> rateGauge = () -> rateLimiter.getRate();
            ActivityMetrics.gauge(activityDef,"targetrate",rateGauge);
            logger.info("targetrate was set to: " + rate);
        });
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    @Override
    public boolean isContiguous() {
        return true;
    }
}
