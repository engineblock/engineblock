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

    public String getCode() { return this.runcode; }

}
