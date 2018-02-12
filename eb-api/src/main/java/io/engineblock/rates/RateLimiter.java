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

public interface RateLimiter {

    /**
     * Reset the internal reference points to the current time, and clear
     * the total delay accumulator. This should be called as close as possible
     * to the actual start time of a limited activity, but not after.
     */
    void start();

    /**
     * Acquire a grant and block until its scheduled time. The return value from
     * this function indicates the scheduling delay for this particular event in
     * nanoseconds, as seen from the rate limiter. It should always be a positive value.
     *
     * @param nanoGrants The number of nanoseconds of schedule time to consume
     * @return The nanosecond scheduling delay after the ideal scheduled time
     */
    long acquire(long nanoGrants);


    /**
     * Like {@link #acquire(long)}, acquire a grant and block until its scheduled
     * time. The return value from this function indicates the scheduling delay
     * for this particular event in nanoseconds, as seen from the rate limiter.
     * It should always be a positive value.
     *
     * Unlike {@link #acquire(long)}, this form assumes a unit of nanotime that
     * would yield the op rate that is set on this rate limiter.
     *
     * @return the nanosecond scheduling delay after the ideal scheduled time
     */
    long acquire();

    /**
     * @return The op rate as set by {@link #setRate(double)}.
     */
    double getRate();

    /**
     * Set the rate for this rate limiter, in ops per second, to be used with
     * {@link #acquire()}. This also has the effect
     * of setting opticks as if {@link #setOpTicks(long)} were called with the equivalent value.
     * For example, if you call this method with 5.0d, then opTicks will be set to 200000000.
     */
    void setRate(double rate);

    /**
     * Return the cumulative scheduling delay since this rate limiter was started.
     * This includes the accumulated delay at different target rates.
     * @return the number of nanoseconds that the actual schedule lags behind the ideal schedule.
     */
    long getCumulativeSchedulingDelayNs();

    /**
     * Return the scheduling delay in nanoseconds since the last time the rate was set
     * on this rate limiter.
     * @return the number of nanoseconds that the actual schedule lags behind the ideal schedule.
     */
    long getCurrentSchedulingDelayNs();

    /**
     * Set the default per-acquire timespan to be used with {@link #acquire()}. This
     * also has the effect of setting the rate as if {@link #setRate(double)} were called
     * with the equivalent value. For example, if you call this method with 100000, rate
     * will be set to 1000d.
     *
     * @param opTicks The minimum number of nanoseconds between each operation.
     */
    void setOpTicks(long opTicks);

    /**
     * @return the minimum number of nanoseconds between each operation.
     */
    long getOpTicks();

}
