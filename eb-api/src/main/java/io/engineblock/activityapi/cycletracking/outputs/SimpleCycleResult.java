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

package io.engineblock.activityapi.cycletracking.outputs;

import io.engineblock.activityapi.cycletracking.buffers.results.CycleResult;
import org.jetbrains.annotations.NotNull;

public class SimpleCycleResult implements CycleResult, Comparable<SimpleCycleResult> {
    private final long cycle;
    private final int result;

    public SimpleCycleResult(long cycle, int result) {
        this.cycle = cycle;
        this.result = result;
    }

    @Override
    public long getCycle() {
        return cycle;
    }

    @Override
    public int getResult() {
        return result;
    }

    @Override
    public int compareTo(@NotNull SimpleCycleResult o) {
        return Long.compare(cycle,o.getCycle());
    }

    public String toString() {
        return this.cycle +"->" + this.result;
    }

}
