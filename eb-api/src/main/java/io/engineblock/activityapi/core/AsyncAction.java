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

import io.engineblock.activityapi.core.ops.fluent.opfacets.TrackedOp;

import java.util.function.LongFunction;

/**
 * <p>An AsyncAction allows an activity type to implement asynchronous
 * operations within each thread.
 * </p>
 *
 */
public interface AsyncAction<D> extends Action {

    LongFunction<D> getOpInitFunction();

    /**
     * THIS DOCUMENTATION IS LIKELY OUT OF DATE
     *
     * The responsibility for tracking async pending against concurrency limits,
     * including signaling for thread state, has been moved into the async
     * event loop of the core motor. If this experiment holds, then the docs
     * here must be rewritten to be accurate for that approach.
     **
     *
     * Enqueue a cycle to be executed by the action. This method should block unless
     * or until the action accepts the cycle to be processed.
     * This method is not allowed to reject a cycle. If it is unable to accept the
     * cycle for any reason, it must throw an exception.
     *
     * Since the action implementation is presumed to be running some externally
     * asynchronous process to support the action, it is up to the action itself
     * to control when to block enqueueing. If the action is not actually asynchronous,
     * then it may need to do downstream processing in order to open room in its
     * concurrency limits for the new cycle.
     *
     * Each action implementation is responsible for tracking and controlling
     * its own limits of concurrency. The {@link BaseAsyncAction} base class is a
     * convenient starting point for such implementations.
     *
     * If the action is known to have additional open slots for an operations to
     * be started (according to the configured concurrency limits),
     * then it can signal such by returning true from this method.
     *
     * @param opc The op context that holds state for this operation
     * @return true, if the action is ready immediately for another operation
     */
    boolean enqueue(TrackedOp<D> opc);

//    /**
//     * Await completion of all pending operations for this thread.
//     * If all tasks are already complete when this is called, then it
//     * should return immediately.
//     * @param timeout Timeout in milliseconds
//     * @return true, if all tasks pending for this thread are completed.
//     */
//    boolean awaitCompletion(long timeout);

//    /**
//     * Once constructed, all async actions are expected to provide a tracker
//     * object which can be used to register callback for operational events,
//     * as well as to provide a diagnostic view of what is happening with
//     * the number of pending operations per thread.
//     * @return An async operations tracker
//     */
//    OpTracker<D> getTracker();

//    /**
//     * When the activity needs to create a new op context which tracks all
//     * things interesting for the operation, it will call this method.
//     * The payload type D determines any and all of what an async action
//     * may know about an op.
//     *
//     * @return A new op state of parameterized type D
//     */
//    D allocateOpData(long cycle);


}
