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

import io.engineblock.activityapi.core.Startable;

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
public class CoreRateLimiter implements RateLimiter, Startable {

    private long opTicks = 0L; // Number of nanos representing one grant at target rate
    private double rate = Double.NaN; // The "ops/s" rate as set by the user

    private long startTimeNanos = System.nanoTime(); //
    private AtomicLong ticksTimeline = new AtomicLong(startTimeNanos);
    private AtomicLong accumulatedDelayNanos = new AtomicLong(0L);
    private AtomicLong lastSeenNanoTime = new AtomicLong(System.nanoTime());

    private volatile boolean started;

    // each blocking call will correct to strict schedule by gap * 1/2^n
    private int limitCompensationShifter = 5;


    /**
     * @param maxOpsPerSecond
     */
    public CoreRateLimiter(double maxOpsPerSecond) {
        this(maxOpsPerSecond, 0.0D);
    }

    /**
     * Create a rate limiter.
     *
     * @param maxOpsPerSecond   Max ops per second
     * @param advanceRatio ratio of limit compensation to advance per acquire.
     */
    public CoreRateLimiter(double maxOpsPerSecond, double advanceRatio) {
        this.setRate(maxOpsPerSecond);
        this.setLimitCompensation(advanceRatio);
    }

    //

    /**
     * See {@link RateLimiter} for interface docs.
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

            // If slower than allowed rate, then fast-forward ticks timeline to
            // close gap by some proportion.
            if (timeSliceDelay > nanos) {
                long gapAnneal = timeSliceDelay >> limitCompensationShifter;
                ticksTimeline.addAndGet(gapAnneal);
            }

            try {
                Thread.sleep(timeSliceDelay / 1000000, (int) (timeSliceDelay % 1000000L));
            } catch (InterruptedException ignoringSpuriousInterrupts) {
                // This is only a safety for spurious interrupts. It should not be hit often.
            }
//            }

//            if (limitCompensationShifter>0) {
//                long gapClose = delayForNanos >> limitCompensationShifter;
//                ticksTimeline.addAndGet(gapClose);
//            }
            // indicate that no cumulative delay is affecting this caller, only execution delay from above
            return 0;
        }
        return timelinePosition - timeSlicePosition;
    }

//    public long acquire(long size, long time) {
//
//    }

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


    @Override
    public synchronized void start() {
        if (!started) {
            this.started = true;
            accumulatedDelayNanos.set(0L);
            resetReferences();
        }
    }

    @Override
    public long getOpTicks() {
        return opTicks;
    }

    @Override
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
        this.rate = rate;
        opTicks = (long) (1000000000d / rate);
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

    public String getSummary() {
        return "rate=" + this.rate + ", " +
                "opticks=" + this.getOpTicks() + ", " +
                "delay=" + this.getCurrentSchedulingDelayNs() + ", " +
                "compensation_shift=" + limitCompensationShifter;
    }


    public int setLimitCompensation(double gapFillRatio) {
        if (gapFillRatio > 1.0D) {
            throw new RuntimeException("gap fill ratio must be between 0.0D and 1.0D");
        }
        if (gapFillRatio == 1.0D) {
            this.limitCompensationShifter = 0;
        } else {
            long longsize = (long) (gapFillRatio * (double) Long.MAX_VALUE);
            this.limitCompensationShifter = Long.numberOfLeadingZeros(longsize);
        }

        return this.limitCompensationShifter;
    }

    public static class Builder implements RateLimiterBuilder.BuilderFacets {

        private double rate;
        private double strictness;

        @Override
        public WithLimit rate(double rate) {
            this.rate = rate;
            return this;
        }

        @Override
        public WithStrictness withStrictLimit() {
            this.strictness = 1.0D;
            return this;
        }

        @Override
        public WithStrictness withAverageLimit() {
            this.strictness = 0.0D;
            return this;
        }

        @Override
        public WithStrictness strictness(double strictness) {
            this.strictness = strictness;
            return this;
        }

        @Override
        public RateLimiter build() {
            return new CoreRateLimiter(rate, strictness);
        }

    }

}
