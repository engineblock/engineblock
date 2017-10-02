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

package io.engineblock.activityapi.cycletracking.buffers.results_rle;

import io.engineblock.activityapi.cycletracking.buffers.results.CycleResult;
import io.engineblock.activityapi.cycletracking.buffers.results.CycleResultsSegment;
import io.engineblock.activityapi.cycletracking.outputs.SimpleCycleResult;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class CycleSpanResults implements CycleResultsSegment {

    private final long min;
    private final long nextMin;
    private final int result;

    public CycleSpanResults(long min, long nextMin, int result) {

        this.min = min;
        this.nextMin = nextMin;
        this.result = result;
    }

    @Override
    public long getCount() {
        return (int) nextMin-min;
    }

    @Override
    public long getMinCycle() {
        return min;
    }

    public String toString() {
        return "[" + min + "," + nextMin + ")->" + result;
    }

    @NotNull
    @Override
    public Iterator<CycleResult> iterator() {
        return new Iter(min,nextMin);
    }

    private class Iter implements Iterator<CycleResult> {
        private final long nextMin;
        private long next;

        public Iter(long min, long nextMin) {
            next = min;
            this.nextMin = nextMin;
        }

        @Override
        public boolean hasNext() {
            return next < nextMin;
        }

        @Override
        public CycleResult next() {
            return new SimpleCycleResult(next++,result);
        }

    }
}
