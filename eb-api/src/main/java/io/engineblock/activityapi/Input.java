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

import java.util.concurrent.atomic.AtomicLong;

/**
 * An Input is the core data source for feeding actions within an activity.
 * Inputs are required to act as sequences at this level. They act as the dataflow control points for
 * banks of motors and actions. As such, they must know their bounds.
 */
public interface Input extends longIntervalSupplier {
    /**
     * @return the minimum value to be provided by this input sequence, AKA
     * the first value that it will provide.
     */
    AtomicLong getMinCycle();

    /**
     * @return the maximum value to be provided by this input sequence
     * before it is considered exhausted.
     */
    AtomicLong getMaxCycle();

    /**
     * @return the current value of the atomic register used by this input.
     */
    long getPendingCycle();

    /**
     * For the sake of efficiency, ActivityMotors that consume values from this interface should do a range check
     * after getting the value. When the value exceeds the the value provided by {@link #getMaxCycle}, the motor should
     * take this as a signal to terminate gracefully with a log line indicating why.
     * @return The next long value in the sequence
     */
    default long getCycle() {
        return getCycleInterval(1);
    }

    /**
     * For the sake of efficiency, ActivityMotors that consume values from this interface
     * should do a range check after getting the value. When the value exceeds the value
     * provided by {@link #getMaxCycle}, the motor should take this as a signal to terminate
     * gracefully with a log line indicating why.
     * @param stride The width of the interval of numbers returned
     * @return the first value of the interval
     */
    long getCycleInterval(int stride);

}
