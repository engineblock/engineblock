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

package io.engineblock.activityapi.core.ops.fluent.opcontext;

public interface TimedOperation {

    /**
     * Clear the state of this op context to the same state as if
     * it were newly constructed. This is meant to make the operation
     * suitable for use by a new cycle without any risk of data from
     * the previous cycle carrying over into another.
     *
     * @return a like-new op context
     */
    TimedOperation reset();

    /**
     * Mark the start time of an operation in system nanoseconds.
     *
     * @return the op context, for method chaining
     */
    TimedOperation start();

    /**
     * Mark the stop time and the result of this operation. This method should be
     * called immediately when the result is known, <em>once and only once</em>.
     *
     * @param result An integer representing the result code for this operation,
     *               to be defined by the caller
     * @return the op context, for method chaining
     */
    TimedOperation stop(int result);

    /**
     * Get the number of nanoseconds that have elapsed between this op being submitted
     * for execution (marked with the {@link #start()} method) and the time that it was
     * marked as complete, with the {@link #stop(int)} method.
     *
     * @return service time in nanoseconds
     */
    long getFinalServiceTime();

    /**
     * Get the number of nanoseconds that have elapsed between this op being submitted
     * for execution (marked with the {@link #start()} method and when this method
     * is called.
     *
     * @return cumulative service so far
     */
    long getCumulativeServiceTime();

    /**
     * Get the number of nanoseconds of service time added to the number of nanoseconds of
     * wait time. This represents the latency associated with coordinated omission.
     *
     * @return total latency in nanoseconds
     */
    long getFinalResponseTime();

    /**
     * Get the number of nanoseconds of service time added to the number of nanoseconds
     * of wait time, as of the time the method is called.
     *
     * @return total response time so far
     */
    long getCumulativeResponseTime();

    /**
     * If this op has been started or restarted, but hasn't been stopped since,
     * then return true. This method is not required to return true for any other condition.
     * @return true, if the op has not been stopped since the last time it was started.
     */
    boolean isRunning();

}

