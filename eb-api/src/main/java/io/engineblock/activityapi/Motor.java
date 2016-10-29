/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package io.engineblock.activityapi;

import io.engineblock.activityimpl.SlotStateTracker;

/**
 * The core threading harness within an activity.
 */
public interface Motor extends Runnable, Stoppable {

    /**
     * Set the input on this motor. It will be read from each cycle before applying the action.
     *
     * @param input an instance of ActivityInput
     * @return this ActivityMotor, for method chaining
     */
    Motor setInput(Input input);

    Input getInput();

    /**
     * Set the action on this motor. It will be applied to each input.
     *
     * @param action an instance of activityAction
     * @return this ActivityMotor, for method chaining
     */
    Motor setAction(Action action);

    Action getAction();

    /**
     * get the slotId which this motor is assigned to within the activity instance.
     * @return long slot id
     */
    long getSlotId();

    /**
     * Get a description of the current slot run status.
     * @return - a value from the {@link SlotState} enum
     */
    SlotStateTracker getSlotStateTracker();

//    SlotState getSlotState();
}
