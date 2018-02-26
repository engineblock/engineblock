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

package io.engineblock.rates;

import com.codahale.metrics.Gauge;
import io.engineblock.activityapi.core.Activity;
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
 * This implementation of the rate limiter is simplified to only support
 * averaging rate limits. The {@link RateLimiters} class provides a
 * convenient way for using either the averaging rate limiter or the
 * strict rate limiter at runtime, while allowing the averaging rate
 * limiter to be used when desired for higher client-side throughput.
 *
 * <p>
 * Note that the ticks accumulator can not rate limit a single event.
 * Acquiring a grant at some nanosecond size simply consumes nanoseconds
 * from the schedule, with the start time of the allotted time span
 * being conceptually aligned with the start time of the requested event.
 * In other words, previous allocations of the timeline determine the start
 * time of a subsequent caller, not the caller itself.
 */
public class AverageRateLimiter implements Startable, RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(AverageRateLimiter.class);

    private long opTicks = 0L; // Number of nanos representing one grant at target rate
    private double rate = Double.NaN; // The "ops/s" rate as set by the user

    private long startTimeNanos = System.nanoTime();

    private AtomicLong ticksTimeline = new AtomicLong(startTimeNanos);
    private AtomicLong accumulatedDelayNanos = new AtomicLong(0L);
    private AtomicLong lastSeenNanoTime = new AtomicLong(System.nanoTime());
    private Gauge<Long> delayGauge;

    private volatile boolean started;

    // each blocking call will correct to strict schedule by gap * 1/2^n

    /**
     * @param activity
     * @param maxOpsPerSecond
     */
    public AverageRateLimiter(ActivityDef def, double maxOpsPerSecond) {
        this(def, maxOpsPerSecond, 0.0D);
    }

    public AverageRateLimiter(ActivityDef def, RateSpec rateSpec) {
        this(def, rateSpec.opsPerSec, rateSpec.strictness);
    }

    /**
     * Create a rate limiter.
     *
     * @param maxOpsPerSecond Max ops per second
     * @param advanceRatio    ratio of limit compensation to advance per acquire.
     */
    public AverageRateLimiter(ActivityDef def, double maxOpsPerSecond, double advanceRatio) {
        this.delayGauge = ActivityMetrics.gauge(def,"cco-delay",new RateLimiters.DelayGauge(this));
        this.setRate(maxOpsPerSecond);
        this.setStrictness(advanceRatio);
    }

    public static AverageRateLimiter createOrUpdate(ActivityDef def, AverageRateLimiter maybeExtant, RateSpec ratespec) {
        if (maybeExtant == null) {
            logger.debug("Creating new rate limiter from spec: " + ratespec);
            return new AverageRateLimiter(def, ratespec);
        }

        maybeExtant.update(ratespec);
        return maybeExtant;
    }

    /**
     * See {@link AverageRateLimiter} for interface docs.
     * effective calling overhead of acquire() is ~20ns
     *
     * @param nanos nanoseconds of time allotted to this event
     * @return nanoseconds that have already elapsed since this event's ideal time
     */
    @Override
    public long acquire(long nanos) {
        long timeSlicePosition = ticksTimeline.getAndAdd(nanos);
        long timelinePosition = lastSeenNanoTime.get();

        // This is a throughput optimization
        if (timelinePosition < timeSlicePosition) {
            timelinePosition = System.nanoTime();
            lastSeenNanoTime.set(timelinePosition);
        }

        long timeSliceDelay = (timeSlicePosition - timelinePosition);

        if (timeSliceDelay > 0L) {
            try {
                Thread.sleep(timeSliceDelay / 1000000, (int) (timeSliceDelay % 1000000L));
            } catch (InterruptedException ignoringSpuriousInterrupts) {
                // This is only a safety for spurious interrupts. It should not be hit often.
            }

            // indicate that no cumulative delay is affecting this caller, only execution delay from above
            return 0;
        }
        return timelinePosition - timeSlicePosition;
    }

    @Override
    public long acquire() {
        return acquire(opTicks);
    }


    @Override
    public long getCumulativeSchedulingDelayNs() {
        return getCurrentSchedulingDelayNs() + accumulatedDelayNanos.get();
    }

    @Override
    public long getCurrentSchedulingDelayNs() {
        return (System.nanoTime() - this.ticksTimeline.get());
    }


    public synchronized void start() {
        if (!started) {
            this.started = true;
            accumulatedDelayNanos.set(0L);
            resetReferences();
        }
    }

    public long getOpTicks() {
        return opTicks;
    }

    public synchronized void setOpTicks(long opTicks) {
        this.opTicks = opTicks;
        this.rate = 1000000000d / opTicks;
        accumulateDelay();
        resetReferences();
    }

    @Override
    public double getRate() {
        return rate;
    }

    @Override
    public synchronized void setRate(double rate) {
        if (rate > 1000000000.0D) {
            throw new RuntimeException("The rate must not be greater than 1000000000. Timing precision is in nanos.");
        }
        if (rate <= 0.0D) {
            throw new RuntimeException("The rate must be greater than 0.0");
        }
        this.rate = rate;
        opTicks = (long) (1000000000d / rate);
        logger.info("OpTicksNs for one cycle is " + opTicks + "ns");

        accumulateDelay();
        resetReferences();
    }

    private void accumulateDelay() {
        accumulatedDelayNanos.addAndGet(getCumulativeSchedulingDelayNs());
    }

    private void resetReferences() {
        long newSetTime = System.nanoTime();
        this.ticksTimeline.set(newSetTime);
        startTimeNanos = newSetTime;
    }

    public String toString() {
        return getSummary();
    }

    @Override
    public String getSummary() {
        return "rate=" + this.rate + ", " +
                "opticks=" + this.getOpTicks() + ", " +
                "delay=" + this.getCurrentSchedulingDelayNs() + ", " +
                "strictness=" + 0.0D;
    }

    /**
     * Set a ratio of scheduling gap which will be closed automatically
     * if it is not used. For the averaging rate limiter, this is always 0.0.
     * An error will be thrown if an attempt is made to change it directly.
     *
     * See {@link RateLimiters#createOrUpdate(ActivityDef, RateLimiter, RateSpec)} as a
     * safe way to change from average rate limiting to strict rate limiting
     * at runtime.
    */
    public int setStrictness(double strictness) {
        if (strictness!=0.0D) {
            throw new RuntimeException("The average rate limiter can only have a strictness of 0.0. See javadoc for details on how to fix this.");
        }
        return 63; // shift any positive long value>>> to 0L;
    }

    public double getStrictness() {
        return 0.0D;
    }

    @Override
    public synchronized void update(RateSpec rateSpec) {
        if (getRate() != rateSpec.opsPerSec) {
            setRate(rateSpec.opsPerSec);
        }
        if (getStrictness() != rateSpec.strictness) {
            throw new RuntimeException("Unable to change the strictness on an averaging rate limiter.");
        }
    }


}
