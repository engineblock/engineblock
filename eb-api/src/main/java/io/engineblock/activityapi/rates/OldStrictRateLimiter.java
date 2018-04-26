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
 * The ticks accumulator can be set to enforce strict isochronous timing
 * from one call to the next, or it can be allowed to dispatch a burst
 * of events as long as the average rate does not exceed the target rate.
 * In practice neither of these approaches is ideal. By default, the
 * scheduling buffer that may result from slow start of callers is
 * gradually removed, thus shifting from an initially bursty rate limiter
 * to a strictly isochronous one. This allows for calling threads to settle
 * in. A desirable feature of this rate limiter will be to add options to
 * limit based on strict limit or average limit.
 *
 * <p>
 * Note that the ticks accumulator can not rate limit a single event.
 * Acquiring a grant at some nanosecond size simply consumes nanoseconds
 * from the schedule, with the start time of the allotted time span
 * being conceptually aligned with the start time of the requested event.
 * In other words, previous allocations of the timeline determine the start
 * time of a new caller, not the caller itself.
 */
public class OldStrictRateLimiter implements Startable,RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(OldStrictRateLimiter.class);
    private final Gauge<Long> delayGauge;

    private long opTicks = 0L; // Number of nanos representing one grant at target rate
    private double rate = Double.NaN; // The "ops/s" rate as set by the user

    private long startTimeNanos = System.nanoTime(); //
    private AtomicLong ticksTimeline = new AtomicLong(startTimeNanos);
    private AtomicLong accumulatedDelayNanos = new AtomicLong(0L);
    private AtomicLong lastSeenNanoTime = new AtomicLong(System.nanoTime());
//    private AtomicLong accumulatedOps = new AtomicLong(0L);

    private volatile boolean started;

    // each blocking call will correct to strict schedule by gap * 1/2^n
    private int limitCompensationShifter = 5;
    private double strictness;


    /**
     * Create a rate limiter.
     *
     * @param maxOpsPerSecond Max ops per second
     * @param advanceRatio    ratio of limit compensation to advance per acquire.
     */
    public OldStrictRateLimiter(ActivityDef def, double maxOpsPerSecond, double advanceRatio) {
        this.delayGauge = ActivityMetrics.gauge(def, "cco-delay", new RateLimiters.DelayGauge(this));
        this.setRate(maxOpsPerSecond);
        this.setStrictness(advanceRatio);
    }

    public OldStrictRateLimiter(ActivityDef def, RateSpec spec) {
        this(def, spec.opsPerSec, spec.strictness);
    }


    /**
     * Initialize this rate limiter instance with previous state, as if continuing on for another
     * rate limiter.
     *
     * @param activity
     * @param spec
     * @param cumulativeSchedulingDelayNs
     */
    public OldStrictRateLimiter(ActivityDef activity, RateSpec spec, long cumulativeSchedulingDelayNs) {
        this(activity, spec);
        this.accumulatedDelayNanos.set(cumulativeSchedulingDelayNs);
    }

    public OldStrictRateLimiter(ActivityDef def, double rate) {
        this(def, rate, 1.0D);
    }

    public static OldStrictRateLimiter createOrUpdate(ActivityDef def, OldStrictRateLimiter maybeExtant, RateSpec ratespec) {
        if (maybeExtant == null) {
            logger.debug("Creating new rate limiter from spec: " + ratespec);
            return new OldStrictRateLimiter(def, ratespec);
        }

        maybeExtant.update(ratespec);
        return maybeExtant;
    }

    /**
     * See {@link OldStrictRateLimiter} for interface docs.
     * effective calling overhead of acquire() is ~20ns
     *
     * @param nanos nanoseconds of time allotted to this event
     * @return nanoseconds that have already elapsed since this event's ideal time
     */
    @Override
    public long acquire(long nanos) {
        long opScheduleTime = ticksTimeline.getAndAdd(nanos);
        long timelinePosition = lastSeenNanoTime.get();

        if (opScheduleTime < timelinePosition) {
            return 0L;
        }

        timelinePosition = System.nanoTime();
        lastSeenNanoTime.set(timelinePosition);
        long scheduleDelay = timelinePosition - opScheduleTime;

        if (scheduleDelay > 0) {
            accumulatedDelayNanos.addAndGet(scheduleDelay);
            return accumulatedDelayNanos.get();
        } else {
            try {
                Thread.sleep(scheduleDelay / 1000000, (int) (scheduleDelay % 1000000L));
            } catch (InterruptedException ignoringSpuriousInterrupts) {
            }
            return 0;
        }
    }


//            if ((timeSlicePosition%10)==0) {
//                // If slower than allowed rate,
//                // then fast-forward ticks timeline to
//                // close gap by some proportion.
//                long gap = (timelinePosition - timeSlicePosition) - nanos;
//                if (gap > 0) {
//                    gap >>>= limitCompensationShifter;
//                    if (gap > 0) {
//                        logger.debug("closing gap by " + gap);
//                        ticksTimeline.addAndGet(gap);
//                    }
//                }
//            }
//        }
//
//        if (delayForNanos>0L) {
//            try {
////                logger.debug("sleeping " + timeSliceDelay);
//                Thread.sleep(delayForNanos / 1000000, (int) (delayForNanos % 1000000L));
//            } catch (InterruptedException ignoringSpuriousInterrupts) {
//                // This is only a safety for spurious interrupts. It should not be hit often.
//            }
//
//            // indicate that no cumulative delay is affecting this caller, only execution delay from above
//            return 0;
//        }
//        return timelinePosition - opScheduleTime;
//    }

    @Override
    public long acquire() {
        return acquire(opTicks);
    }


    @Override
    public long getTotalSchedulingDelay() {
        return getRateSchedulingDelay() + accumulatedDelayNanos.get();
    }

    @Override
    public long getRateSchedulingDelay() {
        return (System.nanoTime() - this.ticksTimeline.get());
    }


    public synchronized void start() {
        if (!started) {
            this.started = true;
            accumulatedDelayNanos.set(0L);
            resetReferences();
        }
    }

    public long getOpNanos() {
        return opTicks;
    }

    public synchronized double setOpNanos(long opTicks) {
        this.opTicks = opTicks;
        this.rate = 1000000000d / opTicks;
        accumulateDelay();
        resetReferences();
        return getRate();
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
        logger.info("OpTicksNs for one cycle is " + opTicks +"ns");

        accumulateDelay();
        resetReferences();
    }

    private void accumulateDelay() {
        accumulatedDelayNanos.addAndGet(getTotalSchedulingDelay());
    }

    private void resetReferences() {
        long newSetTime = System.nanoTime();
        this.ticksTimeline.set(newSetTime);
        startTimeNanos = newSetTime;
    }

    public String toString() {
        return "rate=" + this.rate + ", " +
                "opticks=" + this.getOpNanos() + ", " +
                "delay=" + this.getRateSchedulingDelay() + ", " +
                "strictness=" + limitCompensationShifter;
    }

    /**
     * Set a ratio of scheduling gap which will be closed automatically
     * if it is not used. If this value is 1.0, then scheduling nanos that
     * were not used in real time will be forfeited by the caller. This is
     * use-it-or-lose-it scheduling. If this value is set to 0.0, then the
     * unused time by slow callers will remain on the schedule to be absorbed
     * by bursting or periods of higher-than-rate usage, up to the point at
     * which the average rate is met.
     * <p>
     * This value will be converted to the nearest 1/2N equivalent shift
     * value for fast processing internally. This means that 0.0, 0.5, 0.25, 0.125,
     * and so forth are all valid, but in-between values will be converted to the
     * nearest matching offset. 1.0 is Also valid.
     * </p>
     *
     * @param strictness A value between 0.0 and 1.0 that sets strictness.
     * @return The nano
     */
    public int setStrictness(double strictness) {
        this.strictness = strictness;
        if (strictness > 1.0D) {
            throw new RuntimeException("gap fill ratio must be between 0.0D and 1.0D");
        }
        if (strictness == 1.0D) {
            this.limitCompensationShifter = 0;
        } else {
            long longsize = (long) (strictness * (double) Long.MAX_VALUE);
            this.limitCompensationShifter = Math.min(Long.numberOfLeadingZeros(longsize), 63);
        }

        return this.limitCompensationShifter;
    }

    public double getStrictness() {
        return this.strictness;
    }

    @Override
    public synchronized void update(RateSpec rateSpec) {

        if (getRate() != rateSpec.opsPerSec) {
            setRate(rateSpec.opsPerSec);
        }
        if (getStrictness() != rateSpec.strictness) {
            setStrictness(rateSpec.strictness);
        }
    }

    @Override
    public RateSpec getRateSpec() {

        return null;
    }


}
