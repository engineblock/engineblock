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
package com.metawiring.load.cycler;


import com.metawiring.load.activityapi.ActivityMotor;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Use a strictly-advancing state model to represent the life-cycle of an activity thread.
 */
public class MotorController {

    private final ActivityMotor activityMotor;
    private String slotId;

    public final AtomicReference<RunState> runState = new AtomicReference<>(RunState.Initialized);

    public boolean isStarted() {
        return runState.get() == RunState.Started;
    }

    public static enum RunState {

        // Initial state after creation of this control
        Initialized("I"),
        // This thread is running. This should only be set by the controlled thread
        Started("S"),
        // This thread has completed all of its activity, and will do no further work without new input
        Finished("F"),
        // The thread has been requested to stop. This says nothing of the internal state.
        Stopping("_"),
        // The thread has stopped. This should only be set by the controlled thread
        Stopped(".");

        private String runcode;

        RunState(String runcode) {
            this.runcode = runcode;
        }

        public String getCode() { return this.runcode; }
    }

    public MotorController(ActivityMotor activityMotor) {
        this.activityMotor = activityMotor;
    }

    public MotorController.RunState getRunState() {
        return runState.get();
    }

    public void signalStarted() {
        transitionOrException(RunState.Initialized, RunState.Started);
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
                transitionOrException(RunState.Started, RunState.Stopping);
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
        transitionOrException(RunState.Started, RunState.Finished);
    }

    public void signalStopped() {
        transitionOrException(RunState.Stopping, RunState.Stopped);
    }


    private boolean transition(RunState fromRunState, RunState toRunState) {
        return runState.compareAndSet(fromRunState, toRunState);
    }

    private void transitionOrException(RunState fromRunState, RunState toRunState) {
        if (!runState.compareAndSet(fromRunState, toRunState)) {
            throw new RuntimeException("Unable to transition motor controller " + fromRunState + " --> " + toRunState + ": current state:" + this);
        }
    }

    @Override
    public String toString() {
        return this.runState.get().toString();
    }
}
