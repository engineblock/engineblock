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

import java.util.concurrent.atomic.AtomicLong;

/**
 * See docs.engineblock.io for details on this in the future.
 */
public class RateLimiterCOAware implements EngineBlockRateLimiter {

    private final static long rolloverLimit = 8000000000L;

    private long opTicks = 0L; // Number of nanos representing one grant at target rate
    private double rate= Double.NaN; // The "ops/s" rate as set by the user

    private long startTimeNanos = System.nanoTime(); //
    private AtomicLong ticksTimeline = new AtomicLong(startTimeNanos);
    private AtomicLong accumulatedDelayNanos = new AtomicLong(0L);

    public RateLimiterCOAware(double rate) {
        this.setRate(rate);
    }

    @Override
    public void acquire(long nanos) {
        long minimumTimelinePosition = ticksTimeline.getAndAdd(nanos);
        long timelinePosition = System.nanoTime();
        if (timelinePosition<minimumTimelinePosition) {
            long delayForNanos = minimumTimelinePosition-timelinePosition;
            try {
                Thread.sleep(delayForNanos/1000000,(int) (delayForNanos%1000000L));
            } catch (InterruptedException ignored) {}
        }

    }

    @Override
    public void acquire() {
        acquire(opTicks);
    }

    @Override
    public double getRate() {
        return rate;
    }

    /**
     * @return the expected number of ticks in nanos that each op should accumulate
     */
    public long getOpTicks() {
        return opTicks;
    }

    /**
     * @return the nano ticks which have not been acquired since the start of the timer.
     */
    public long getDelayNanos() {
        return (System.nanoTime() - this.ticksTimeline.get()) + accumulatedDelayNanos.get();
    }

    @Override
    public void setRate(double rate) {
        long newSetTime = System.nanoTime();
        accumulatedDelayNanos.addAndGet(getDelayNanos());
        this.ticksTimeline.set(newSetTime);
        startTimeNanos = newSetTime;
        this.rate = rate;
        opTicks = (long)(1000000000d/rate);
    }

}
