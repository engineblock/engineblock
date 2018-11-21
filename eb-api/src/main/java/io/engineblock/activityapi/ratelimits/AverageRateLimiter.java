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

import com.codahale.metrics.Gauge;
import io.engineblock.activityapi.core.Startable;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.ActivityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class AverageRateLimiter implements Startable, RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(AverageRateLimiter.class);
    private String label;
    private ActivityDef activityDef;
    private RateSpec rateSpec;
    private long opTicks = 0L; // Number of nanos representing one grant at target rate
    protected final AtomicLong ticksTimeline = new AtomicLong(0L);
    private final AtomicLong accumulatedDelayNanos = new AtomicLong(0L);
    private volatile long lastSeenNanoTime;
    private volatile boolean isBursting = false;

    private State state = State.Idle;

    //private Counter fastpathCounter;
    //private Counter sleepCounter;
    private Gauge<Long> delayGauge;
    private Gauge<Double> avgRateGauge;
    //private Gauge<Double> burstRateGauge;

    protected AverageRateLimiter() {
    }

    /**
     * Create a rate limiter.
     *
     * @param def The activity definition for this rate limiter
     * @param label The label for the rate limiting facet within the activity
     * @param rateSpec the rate limiter configuration
     */
    public AverageRateLimiter(ActivityDef def, String label, RateSpec rateSpec) {
        setActivityDef(def);
        setLabel(label);
        this.setRateSpec(rateSpec);
        init();
    }

    protected void setLabel(String label) {
        this.label = label;
    }

    protected void setActivityDef(ActivityDef def) {
        this.activityDef = def;
    }

    protected void init() {
        this.delayGauge = ActivityMetrics.gauge(activityDef, label+".cco_delay_gauge", new RateLimiters.WaitTimeGuage(this));
        //this.sleepCounter = ActivityMetrics.counter(activityDef, label + "_ratelogic.sleep_counter");
        //this.fastpathCounter = ActivityMetrics.counter(activityDef, label + "_ratelogic.fast_counter");
        this.avgRateGauge = ActivityMetrics.gauge(activityDef, label + ".avg_targetrate_gauge", new RateLimiters.RateGauge(this));
        //this.burstRateGauge = ActivityMetrics.gauge(activityDef, label + "_ratelogic.burst_targetrate_gauge", new RateLimiters.BurstRateGauge(this));
        start();
    }

    protected long getNanoClockTime() {
        return System.nanoTime();
    }

    @Override
    public long acquire() {
        long opScheduleTime = ticksTimeline.getAndAdd(opTicks);
        long timelinePosition = lastSeenNanoTime;

        if (opScheduleTime < timelinePosition) {
            isBursting = true;
            return timelinePosition - opScheduleTime;
        }

        long delay = timelinePosition - opScheduleTime;
        if (delay <= 0) {

            timelinePosition = getNanoClockTime();
            lastSeenNanoTime = timelinePosition;
            delay = timelinePosition - opScheduleTime;

            // This only happens if the callers are ahead of schedule
            if (delay < 0) {
                isBursting = false;
                delay *= -1;
                try {
                    //sleepCounter.inc();
                    Thread.sleep(delay / 1000000, (int) (delay % 1000000L));
                } catch (InterruptedException ignored) {
                }
                return 0L;
            }
        }

        return delay;
    }


    public long getOpNanos() {
        return opTicks;
    }

    @Override
    public long getTotalWaitTime() {
        return getWaitTime() + accumulatedDelayNanos.get();    }

    @Override
    public long getWaitTime() {
        return getNanoClockTime() - ticksTimeline.get();    }


    public synchronized void start() {
        switch (state) {
            case Started:
                break;
            case Idle:
                sync();
                state = State.Started;
                break;
        }
    }

    private synchronized void sync() {
        long nanos = getNanoClockTime();
        this.lastSeenNanoTime = nanos;

        switch (this.state) {
            case Idle:
                this.ticksTimeline.set(nanos);
                this.accumulatedDelayNanos.set(0L);
                break;
            case Started:
                accumulatedDelayNanos.addAndGet(getWaitTime());
                break;
        }
    }

    public String toString() {
        return "spec=[" + label + "]:" + rateSpec.toString() +
                ", delay=" + this.getWaitTime() +
                ", total=" + this.getTotalWaitTime() +
                ", (used/seen)=(" + ticksTimeline.get() + "/" + lastSeenNanoTime + ")" +
                ", (clock,actual)=(" + getNanoClockTime() + "," + System.nanoTime() + ")";
    }

    @Override
    public RateSpec getRateSpec() {
        return this.rateSpec;
    }

    @Override
    public void setRateSpec(RateSpec updatingRateSpec) {
        RateSpec oldRateSpec = this.rateSpec;
        this.rateSpec=updatingRateSpec;

        if (oldRateSpec!=null && oldRateSpec.equals(this.rateSpec)) {
            return;
        }

        this.opTicks = updatingRateSpec.getNanosPerOp();
        this.rateSpec = updatingRateSpec;
        switch (this.state) {
            case Started:
                sync();
            case Idle:
        }
    }

    /**
     * * visible for testing
     *
     * @return ticksTimeline value - the long value of the shared timeslice marker
     */
    public AtomicLong getTicksTimeline() {
        return this.ticksTimeline;
    }

    /**
     * visible for testing
     *
     * @return lastSeenNanoTime - the long value of the shared view of the clock
     */
    public long getLastSeenNanoTimeline() {
        return this.lastSeenNanoTime;
    }

    private enum State {
        Idle,
        Started
    }
}
