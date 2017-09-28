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

package io.engineblock.activityapi.cycletracking.markers;

import io.engineblock.activityapi.cycletracking.buffers.CycleSegment;

/**
 * A cycle marker is simply a type that knows how to do something
 * useful with the result of a particular cycle.
 */
public interface Marker extends AutoCloseable {

    /**
     * Mark the result of the numbered cycle with an integer value.
     * The meaning of the value provided is contextual to the way it is used.
     * (Each process will have its own status tables, etc.)
     *
     * @param completedCycle The cycle number being marked.
     * @param result the result ordinal
     */
    boolean onCycleResult(long completedCycle, int result);

    default void onCycleSegment(CycleSegment segment) {
        for (int i = 0 ; i<segment.codes.length; i++) {
            onCycleResult(segment.cycle +i, segment.codes[i]);
        }
    }

}
