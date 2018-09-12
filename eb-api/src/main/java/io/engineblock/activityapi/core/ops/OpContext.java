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

package io.engineblock.activityapi.core.ops;

import io.engineblock.activityapi.core.ActivityType;
import io.engineblock.activityapi.core.AsyncAction;
import io.engineblock.activityapi.core.BaseAsyncAction;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleMutable;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResult;

/**
 * This is the way that you track and interact with the state and status of
 * asynchronous operations. This class is expected to be overridden on a
 * per activity basis in order to contain the details needed.
 *
 * In general, you do these things in order with an op context in your activity:
 * <OL>
 *     <LI>{@link #start()} - <em>DO</em> call this first when your action is asked to start an operation.
 *     This is how you indicate the moment that an operation is officially started.</LI>
 *     <LI>{@link #retry()} - <em>MAYBE</em> call this if or when you need
 *     to retry an operation without configuring it again. This resets the service timers.</LI>
 *     <LI>{@link #stop(int)} - <em>DO</em> call this exactly once from your action, after your operation
 *     has completed, and then do nothing else with the op context for this cycle.</LI>
 * </OL>
 *
 * The default implementation of this interface is {@link BaseAsyncAction}. It is recommended that implementors
 * use that as the base op context type when implementing {@link AsyncAction} for an {@link ActivityType}.
 */
public interface OpContext extends
        ScheduledOperation,
        TimedOperation,
        RetryableOperation,
        CycleMutable,
        CycleResult {

    /**
     * Provide a unique identifier for a context which allows for easier diagnostics.
     * @return a context identifier that is guaranteed to be unique for this context.
     */
    long getCtxId();

    /**
     * {@see ScheduledOperation}
     */
    OpContext setWaitTime(long waitTimeNanos);

    /**
     * Add an observer to state changes on this op context. Each opEvents can implement which methods
     * it needs.
     * @param opEvents A {@link OpEvents} implementor.
     * @return the op context, for method chaining.
     */
    OpContext addSink(OpEvents opEvents);

    public static interface OpEvents {
        default void onOpReset(OpContext opc) {}
        default void onOpStart(OpContext opc) {};
        default void onOpRestart(OpContext opc) {};

        /**
         * This event handler is triggered when an OpContext has been stopped, but after timing
         * data has been updated. This means tha the op should be observable in its final
         * state.
         * @param opc the OpContext which was stopped
         */
        default void onAfterOpStop(OpContext opc) {};
    }

}

