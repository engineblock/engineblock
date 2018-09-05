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

/**
 * <p>An AsyncAction allows an activity type to implement asynchronous
 * operations within each thread.
 * </p>
 *
 * <p>
 * An action that does not have the ability to asynchronously submit work
 * in an unblocking fashion should not use this {@link AsyncAction} API.
 * In other words, the action should not defer work for which is needed before
 * an operation is effectively submitted or offloaded to the target system.
 * This would degrade the accuracy of timing metrics.
 * In such cases, the action should implement the {@link SyncAction} API instead.
 * </p>
 */
public interface AsyncAction<T extends OpContext> extends Action {

    /**
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
     * If the action is known to have additional headroom according to the configured
     * concurrency limits, it can signal such by returning true from this method.
     *
     * @param opc
     * @return true, if the action is ready immediately for another operation
     */
    boolean enqueue(T opc);

    /**
     * Await completion of all pending operations for this thread.
     * If all tasks are already complete when this is called, then it
     * should return immediately.
     * @param timeout Timeout in milliseconds
     * @return true, if all tasks pending for this thread are completed.
     */
    boolean awaitCompletion(long timeout);

    /**
     * Since each activity type may want to specialize the op context, each activity
     * is responsible for generating new op contexts.
     * @return a new op context or a sup-type thereof
     */
    default OpContext newOpContext() {
        return new BaseOpContext();
    }

}
