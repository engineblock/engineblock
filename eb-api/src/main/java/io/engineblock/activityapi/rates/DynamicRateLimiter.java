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

import com.codahale.metrics.Gauge;
import io.engineblock.activityapi.core.Startable;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.ActivityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>This rate limiter uses nanoseconds as the unit of timing. This
 * works well because it is the native precision of the system timer
 * interface via {@link System#nanoTime()}. It is also low-error
 * in terms of rounding between floating point rates and nanoseconds,
 * at least in the round numbers that users tend to use. Further,
 * the current scheduling state is maintained as an atomic view of
 * accumulated nanoseconds granted to callers -- referred to here as the
 * ticks accumulator. This further simplifies the implementation by
 * allowing direct comparison of scheduled times with the current
 * state of the high-resolution system timer.
 *
 * <p>
 * Each time {@link #acquire()} or {@link #acquire(long)} is called,
 * a discrete scheduled time is calculated from the current state of
 * the ticks accumulator. If the calculated time is in the future,
 * then the method blocks (in the calling thread) using
 * {@link Thread#sleep(long, int)}. Finally, the method is unblocked,
 * and the nanosecond scheduling gap is returned to the caller.
 *
 * <p>
 * This implementation of the rate limiter is meant to provide a sliding
 * scale between strict rate limiting and average rate limiting.
 * In general it is expected to behave like a strict rate limiter when
 * the rate is near the target rate. When the achieved rate is much
 * lower, then some allowance is made to allow it to catch up according
 * to the strictness setting.
 *
 * <p>
 * Note that the ticks accumulator can not rate limit a single event.
 * Acquiring a grant at some nanosecond size simply consumes nanoseconds
 * from the schedule, with the start time of the allotted time span
 * being conceptually aligned with the start time of the requested event.
 * In other words, previous allocations of the timeline determine the start
 * time of a subsequent caller, not the caller itself.
 */
@SuppressWarnings("ALL")
public class DynamicRateLimiter implements Startable, RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(DynamicRateLimiter.class);
    private String label;
    private ActivityDef activityDef;
    private RateSpec rateSpec;
    private long maxFreeOps = 10;

    private long opTicks = 0L; // Number of nanos representing one grant at target rate
    private long burstTicks;
    protected final AtomicLong allocatedNanos = new AtomicLong(0L);
    private final AtomicLong waitTimeNanos = new AtomicLong(0L);

    private volatile long freeOps = 0L;
    private volatile long lastSeenNanoTime = 0L;

    private State state = State.Idle;

    private Gauge<Long> delayGauge;
    private Gauge<Double> avgRateGauge;

    protected DynamicRateLimiter() {
    }

    /**
     * Create a rate limiter.
     *
     * @param def      The activity definition for this rate limiter
     * @param label    The label for the rate limiting facet within the activity
     * @param rateSpec the rate limiter configuration
     */
    public DynamicRateLimiter(ActivityDef def, String label, RateSpec rateSpec) {
        setActivityDef(def);
        setLabel(label);
        this.setRateSpec(rateSpec);
        this.maxFreeOps = (long) rateSpec.opsPerSec / 10;
        init();
    }

    protected void setLabel(String label) {
        this.label = label;
    }

    protected void setActivityDef(ActivityDef def) {
        this.activityDef = def;
    }

    protected void init() {
        this.delayGauge = ActivityMetrics.gauge(activityDef, label + ".cco_delay_gauge", new RateLimiters.DelayGauge(this));
        this.avgRateGauge = ActivityMetrics.gauge(activityDef, label + ".avg_targetrate_gauge", new RateLimiters.RateGauge(this));
        start();
    }

    protected long getNanoClockTime() {
        return System.nanoTime() + 25; // Typical calling overhead time, plus half phase-ish
    }

    /**
     * See {@link DynamicRateLimiter} for interface docs.
     * effective calling overhead of acquire() is ~20ns
     *
     * @param nanos nanoseconds of time allotted to this event
     * @return nanoseconds that have already elapsed since this event's ideal time
     */
    @Override
    public long acquire(long nanos) {

        long operationScheduledAt = allocatedNanos.getAndAdd(nanos);
        long elapsedTimeCheckpoint = lastSeenNanoTime;

        long opdelay = elapsedTimeCheckpoint - operationScheduledAt;

        if (opdelay > 0) {
            return opdelay;
        }

        if ((freeOps += 1) < maxFreeOps) {
            return 0;
        }
        freeOps = 0L;

        elapsedTimeCheckpoint = getNanoClockTime();
        lastSeenNanoTime = elapsedTimeCheckpoint;
        opdelay = elapsedTimeCheckpoint - operationScheduledAt;

        // This only happens if the callers are ahead of schedule
//            if (behindschedule < 0) {
        if (opdelay < -700) {
            opdelay *= -1;
            try {
                Thread.sleep(opdelay / 1000000, (int) (opdelay % 1000000L));
            } catch (InterruptedException ignored) {
            }
            opdelay = 0L;
        }

        return opdelay;

    }

    @Override
    public long acquire() {
        return acquire(opTicks);
    }

    public long getOpNanos() {
        return opTicks;
    }

    @Override
    public double getRate() {
        return rateSpec.opsPerSec;
    }

    @Override
    public synchronized void setRate(double rate) {
        setRateSpec(this.rateSpec.withOpsPerSecond(rate));
    }

    @Override
    public long getTotalSchedulingDelay() {
        return getRateSchedulingDelay() + waitTimeNanos.get();
    }

    protected long getRateSchedulingDelay() {
        return getNanoClockTime() - allocatedNanos.get();
    }


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
                this.allocatedNanos.set(nanos);
                this.waitTimeNanos.set(0L);
                break;
            case Started:
                waitTimeNanos.addAndGet(getRateSchedulingDelay());
                break;
        }
    }

    public String toString() {
        return "spec=[" + label + "]:" + rateSpec.toString() +
                ", delay=" + this.getRateSchedulingDelay() +
                ", total=" + this.getTotalSchedulingDelay() +
                ", (used/seen)=(" + allocatedNanos.get() + "/" + lastSeenNanoTime + ")" +
                ", (clock,actual)=(" + getNanoClockTime() + "," + System.nanoTime() + ")";
    }

    @Override
    public RateSpec getRateSpec() {
        return this.rateSpec;
    }

    @Override
    public void setRateSpec(RateSpec updatingRateSpec) {
        RateSpec oldRateSpec = this.rateSpec;
        this.rateSpec = updatingRateSpec;

        if (oldRateSpec != null && oldRateSpec.equals(this.rateSpec)) {
            return;
        }

        this.opTicks = updatingRateSpec.getCalculatedNanos();
        this.burstTicks = (long) (updatingRateSpec.getBurstRatio() * (1.0D / opTicks));
        switch (this.state) {
            case Started:
                sync();
            case Idle:
        }
    }

    /**
     * * visible for testing
     *
     * @return allocatedNanos value - the long value of the shared timeslice marker
     */
    public AtomicLong getAllocatedNanos() {
        return this.allocatedNanos;
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
