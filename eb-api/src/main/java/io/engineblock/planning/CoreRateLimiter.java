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

package io.engineblock.planning;

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

    // TODO: Add a mode to allow average rate limit or strict rate limit
    // TODO: Consider calling nanoTime only when needed (in cases which may need time to advance)
    // TODO: Consider adding a leavening value to adjust from average limit to strict over time

    private long opTicks = 0L; // Number of nanos representing one grant at target rate
    private double rate = Double.NaN; // The "ops/s" rate as set by the user

    private long startTimeNanos = System.nanoTime(); //
    private AtomicLong ticksTimeline = new AtomicLong(startTimeNanos);
    private AtomicLong accumulatedDelayNanos = new AtomicLong(0L);
    private volatile boolean started;
//    private volatile long lastDelay;


    public CoreRateLimiter(double maxOpsPerSecond) {
        this.setRate(maxOpsPerSecond);
    }

    /**
     * The default rate limit is 1E9 ops/s.
     */
    public CoreRateLimiter() {
        this(1000000000.0);
    }

    //

    /**
     * See {@link RateLimiter} for interface docs.
     * effective calling overhead of acquire() is ~20ns
     * @param nanos nanoseconds of time allotted to this event
     * @return nanoseconds that have already elapsed since this event's ideal time
     */
    @Override
    public long acquire(long nanos) {
        long minimumTimelinePosition = ticksTimeline.getAndAdd(nanos);
        long timelinePosition = System.nanoTime();

        while (timelinePosition < minimumTimelinePosition) {
//            long delayForNanos = (minimumTimelinePosition - timelinePosition)+35; // ballpark calling overhead
            long delayForNanos = (minimumTimelinePosition - timelinePosition);
            try {
                Thread.sleep(delayForNanos / 1000000, (int) (delayForNanos % 1000000L));
            } catch (InterruptedException ignored) {
                // This is only a safety for spurious interrupts. It should not be hit often.
                timelinePosition = System.nanoTime();
                continue;
            }
            // indicate that no cumulative delay is affecting this caller, only execution delay from above
            return 0;
        }
        return timelinePosition - minimumTimelinePosition;
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


    public String getSummary() {
        return "rate=" + this.rate + ", " +
                "opticks=" + this.getOpTicks() + ", " +
                "delay=" + this.getCurrentSchedulingDelayNs();
    }
}
