/*
*   Copyright 2015 jshook
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
package io.engineblock.activitycore;


import io.engineblock.activityapi.Motor;
import io.engineblock.activityapi.SlotState;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Use a strictly-advancing state model to represent the life-cycle of an activity thread.
 */
public class MotorController {

    private final Motor motor;
    private String slotId;

    public final AtomicReference<SlotState> runState = new AtomicReference<>(SlotState.Initialized);

    public boolean isStarted() {
        return runState.get() == SlotState.Started;
    }


    public MotorController(Motor motor) {
        this.motor = motor;
    }

    public SlotState getRunState() {
        return runState.get();
    }

    public void signalStarted() {
        transitionOrException(SlotState.Initialized, SlotState.Started);
    }

    /**
     * Multiple threads may request a stop, including the controlled thread itself.
     * Allow this to return without exception if the target state is already set.
     */
    public void requestStop() {
        switch (getRunState()) {
            case Finished:
                break;
            case Started:
                transitionOrException(SlotState.Started, SlotState.Stopping);
                break;
            case Stopping:
                break;
            case Stopped:
                break;
            default:
                throw new RuntimeException("Invalid control state to request stop: " + this);
        }
    }

    public void signalFinished() {
        transitionOrException(SlotState.Started, SlotState.Finished);
    }

    public void signalStopped() {
        transitionOrException(SlotState.Stopping, SlotState.Stopped);
    }


    private boolean transition(SlotState fromRunState, SlotState toRunState) {
        return runState.compareAndSet(fromRunState, toRunState);
    }

    private void transitionOrException(SlotState fromRunState, SlotState toRunState) {
        if (!runState.compareAndSet(fromRunState, toRunState)) {
            throw new RuntimeException("Unable to transition motor controller " + fromRunState + " --> " + toRunState + ": current state:" + this);
        }
    }

    @Override
    public String toString() {
        return this.runState.get().toString();
    }
}
