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
import io.engineblock.activityapi.cyclelog.outputs.MutableCycleResult;

public interface AsyncAction extends Action {

    /**
     * Enqueue a cycle to be processed by the action. As long as this method
     * signals more work is being accepted, the calling motor should
     * enqueue more work.
     * @param unprocessed contain the cycle number which is the basis for the action
     * @return true, if the action is accepting more work.
     */
    boolean enqueue(MutableCycleResult unprocessed);

    /**
     * Dequeue a finished unit of work from the action. This method should be called
     * only once before attempting to enqueue again.
     * @return a CycleResult, or null signifying no further results are available
     */
    CycleResult dequeue();
}
