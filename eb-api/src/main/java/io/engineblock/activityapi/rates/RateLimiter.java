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

import io.engineblock.activityapi.core.Startable;

public interface RateLimiter extends Startable {

    /**
     * Block until it is time for the next operation, according to number
     * of nanoseconds allotted to this individual op.
     * @param nanos Nanos to schedule for
     * @return the number of nanos behind schedule when this method returns
     */
    long acquire(long nanos);

    /**
     * Block until it is time for the next operation, according to the
     * nanoseconds per op as set by either {@link #setRate(double)} or
     * {@link #setOpNanos(long)}
     * @return the number of nanos behind schedule when this op returns
     */
    long acquire();

    /**
     * Return the total number of nanoseconds behind schedule
     * that this rate limiter is, including the full history across all
     * rates. When the rate is changed, this value is check-pointed to
     * an accumulator and also included in any subsequent measurement.
     * @return nanoseconds behind schedule since the rate limiter was started
     */
    long getTotalSchedulingDelay();

    /**
     * Get the scheduling delay since the rate was last set or changed.
     * @return nanoseconds behind schedule of the current rate limiter.
     */
    long getRateSchedulingDelay();

    /**
     * Set the rate in ops/s. This is a friendly way to calculate the
     * nanoseconds per op.
     * @param rate The desired ops/s rate.
     */
    void setRate(double rate);

    /**
     * Set the nanoseconds to be given to each operation by default.
     * @param nanos The number of nanoseconds per op
     * @return The equivalent rate in terms of ops/s
     */
    double setOpNanos(long nanos);

    /**
     * Return the rate in ops/s.
     * @return ops/s
     */
    double getRate();

    /**
     * Get the number of nanoseconds allotted to each operations.
     * @return nanosecond op length
     */
    long getOpNanos();

    /**
     * Modify the rate of a running rate limiter.
     * @param spec The rate and strictness specification
     */
    void update(RateSpec spec);

}
