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
public interface AsyncAction extends Action {

    /**
     * <p>
     * Enqueue a cycle to be processed by the action. As long as this method
     * signals more work is being accepted, the calling motor should
     * enqueue more work. An action is never allowed to reject work. It is only
     * allowed to signal whether or not the caller should continue to submit
     * more work. Once the async action signals that the internal buffer is
     * full, it should not be called again until {@link #dequeue()} is called at least
     * once. The enqueued cycle should be processed by the {@link AsyncAction} as
     * an unblocking call before returning.
     * </p>
     *
     * <p>If the method implementation is not able to accept another request for any
     * reason, it MUST throw an exception instead of returning null.</p>
     *
     * @param unprocessed contain the cycle number which is the basis for the action
     * @return true, if the action is accepting more work.
     */
    boolean enqueue(OpContext unprocessed);

    /**
     * Dequeue a finished unit of work from the action. This method should be called
     * only once before attempting to enqueue again.
     * The {@link AsyncAction} is required to block when this method is called until it
     * can produce a cycle result corresponding to one of the enqueued cycles via
     * {@link #enqueue(OpContext)}}. If there are no remaining results to be
     * returned, then null will be returned. This will remain the case unless or untill
     * the caller submits further work to the {@link AsyncAction}. Once the caller is
     * done calling {@link #enqueue(OpContext)}, this method
     * should be called to drain all pending results.*
     *
     * <p>Before an {@link OpContext} is returned by this method is is expected that
     * the implementor call {@link OpContext#setResult(int)}. This causes the status
     * to be propagated to the owning stride as well as for any downstream outputs.
     * If the implementation has a source of external task completion that may occur
     * before the motor calls this method, then the result should be set via
     * callback whenever possible. This will keep op service
     * times accurate in spite of any queueing delays between the motor and the action
     * implementation.
     * </p>
     *
     * <p>Design note: A mutable return type is used here that is the same as the enqueued
     * carrier type to allow for recycling of the carrier object. If the implementing
     * Action simply sets state on the provided type on {@link #enqueue(OpContext)},
     * rather than creating new instances, then heap pressure will be reduced.
     * </p>
     *
     * @return a CycleResult, or null signifying no further results are available unless or until {@link #enqueue(OpContext)} is called again.
     */
    OpContext dequeue();
}
