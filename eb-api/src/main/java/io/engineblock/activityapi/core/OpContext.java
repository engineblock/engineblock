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

package io.engineblock.activityapi.core;

import io.engineblock.activityapi.cyclelog.buffers.results.CycleResult;

/**
 * This is the way that you track and interact with the state and status of
 * asynchronous operations. This class is expected to be overridden on a
 * per activity basis in order to contain the details needed.
 *
 * In general, you do these things in order with an op context in your activity:
 * <OL>
 *     <LI>{@link #init(Sink, long, long)} - This is done for you by the runtime. Do <em>not</em> do this yourself.</LI>
 *     <LI>{@link #start()} - <em>DO</em> call this first. This is how you indicate the moment that an operation is officially started.</LI>
 *     <LI>{@link #restart()} - <em>MAYBE</em> call this, if you need to restart or retry an op.</LI>
 *     <LI>{@link #stop(int)} - <em>DO</em> call this exactly once, and then do nothing else with the op context for this cycle.</LI>
 * </OL>
 *
 * This class works well with the {@link BaseAsyncAction} class, requiring that you implement only
 * {@link BaseAsyncAction#startOpCycle(OpContext)}, handle any downstream effects, and then ultimately call {@link #stop(int)} exactly once.
 * By doing so, you signal to the runtime that the operation is complete, and the built-in status handling and context recycling
 * will handle the rest.
 */
public interface OpContext extends CycleResult {

    /**
     * Clear the init timer for the operation.
     * @return the op context, with a negative or zero init time.
     */
    OpContext reset();

    /**
     * Mark the init time of an operation, the cycle which it is associated with, and
     * the scheduling delay. This is normally done by the motor, not the action.
     *
     * @param sink The object which consumed the result of this op when it is finally stopped.
     * @param cycle The cycle which the operation is associated with.
     * @param delayNanos The scheduling delay for this op, relative to the ideal scheduling time
     * @return the op context, for method chaining
     */
    OpContext init(Sink sink, long cycle, long delayNanos);

    /**
     * Mark the init of an operation which is being retried or otherwise restarted.
     * If this represents an execution that was attempted by the activity, but which was
     * not completed for some reason, a metric which counts this reason must be provided
     * in that activity type's documentation.
     *
     * Alternately, this may be called by an action in order to disregard any setup time
     * that might occur
     * @return the op context, for method chaining
     */
    OpContext start();

    OpContext restart();

    /**
     * Mark the result of this op. This method should be called by the owning action
     * as soon as the result is known, since this also has the side-effect of marking
     * the op completion time as well as informing the Sink of the result status.
     *
     * IMPORTANT: This method should only be called once for the duration of the operation.
     * This is considered the marking event for the final status of an operation.
     * <em>There can be only one.</em>
     *
     * IMPORTANT: This method is expected to {@link Object#notifyAll()} when it is called.
     *
     * @param result An integer representing the result code for this operation, according to the
     *               result map for the owning activity.
     * @return the op context, for method chaining
     */
    OpContext stop(int result);

    /**
     * Get the number of nanoseconds that have elapsed between this op being submitted
     * for execution (marked with the {@link #start()} method)
     * and the time that it was marked as complete, with the {@link #stop(int)} method.
     * @return service time in nanoseconds
     */
    long getServiceTime();

    /**
     * Get the number of nanoseconds that have elapsed between this op being submitted
     * for execution (marked with the {@link #start()} method)
     * and the current system nano time.
     *
     * This is useful for getting a running timer which is calculated on the fly for what total
     * service time would be if the op were stopped at the provided system nanos time.
     * @return service time in nanoseconds, as of currentTimeNanos
     */
    long getCumulativeServiceTime();

    /**
     * Get the number of nanoseconds of service time added to the number of nanoseconds of
     * scheduling delay. This represents the latency associated with coordinated omission.
     * @return total latency in nanoseconds
     */
    long getFinalResponseTime();

    /**
     * Get the number of nanoseconds of service time added to the number of nanoseconds
     * of scheduling delay, as of the time the method is called.
     *
     * This is useful for getting a running timer which is calculated on the
     * fly for what total latency would be, without affecting the reportable
     * state of the op context.
     * @return total latency as of currentTimeNanos
     */
    long getCumulativeResponseTime();

    /**
     * Get the number of nanoseconds between the time that this operation was scheduled to run,
     * and when it was initiated.
     * @return nanoseconds of wait time
     */
    long getWaitTime();

    /**
     * Get the number of times start or restart were called, cumulatively, since the last reset.
     * @return total attempts to complete this op
     */
    int getTries();

    /**
     * Provide a unique identifier for a context which allows for easier diagnostics.
     * @return a context identifier that is guaranteed to be unique for this context.
     */
    long getCtxId();

    /**
     * If this op has been started or restarted, but hasn't been stopped since,
     * then return true. This method is not required to return true for any other condition.
     * @return true, if the op has not been stopped since the last time it was started.
     */
    boolean isRunning();

    /**
     * This interface is what consumers of op contexts must implement.
     */
    public static interface Sink {
        void handle(OpContext opc);
    }

}

