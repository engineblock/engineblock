/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.engineblock.activityapi;

public enum SlotState {

    // Initial state after creation of this control
    Initialized("I>"),
    // This thread is running. This should only be set by the controlled thread
    Started("S>"),
    // This thread has completed all of its activity, and will do no further work without new input
    Finished("F."),
    // The thread has been requested to stop. This says nothing of the internal state.
    Stopping("-\\"),
    // The thread has stopped. This should only be set by the controlled thread
    Stopped("_.");

    private String runcode;

    SlotState(String runcode) {
        this.runcode = runcode;
    }

    public String getCode() {
        return this.runcode;
    }

    public boolean canTransitionTo(SlotState to) {
        switch (this) {
            default:
                return false;
            case Initialized: // A motor was just created. This is its initial state.
                switch (to) {
                    case Started: // a motor has been executed after being initialized
                        return true;
                    default:
                        return false;
                }
            case Started:
                switch (to) {
                    case Stopping: // A request was made to stop the motor before it finished
                    case Finished: // A motor has exhausted its input, and is finished with its work
                        return true;
                    default:
                        return false;
                }
            case Stopping:
                switch (to) {
                    case Stopped: // A motor was stopped by request before exhausting input
                        return true;
                    default:
                        return false;
                }
            case Stopped:
                switch (to) {
                    case Started: // A motor was restarted after being stopped
                        return true;
                    default:
                        return false;
                }
            case Finished:
                switch (to) {
                    case Started:
                        return true;
                    // not useful as of yet.
                    // Perhaps this will be allowed via explicit reset of input stream.
                    // If the input isn't reset, then trying to start a finished motor
                    // will cause it to short-circuit back to Finished state.
                    default:
                        return false;
                }
        }

    }

}
