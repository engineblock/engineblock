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

package io.engineblock.activityapi;

/**
 * A continuing action can ask to be iterated within the cycle.
 * <p>
 * Motors must:
 * <ul>
 * <li>Detect when an action is a multi-phase action at initialization time, not in the inner loop</li>
 * <li>Call accept(cycle) as normal.</li>
 * <li>If the action is a multi-phase action, keep calling accept(cycle), with the same cycle number as above,
 * until incomplete returns false.</li>
 * </ul>
 */
public interface MultiPhaseAction extends Action {

    /**
     * Signal to the caller whether or not the current multi-phase is completed.
     *
     * @return true when completed.
     */
    boolean incomplete();
}
