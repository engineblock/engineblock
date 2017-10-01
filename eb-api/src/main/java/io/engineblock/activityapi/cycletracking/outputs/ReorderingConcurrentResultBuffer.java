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

import io.engineblock.activityapi.cycletracking.buffers.results.CycleResultsSegment;
import io.engineblock.activityapi.cycletracking.buffers.results.CycleResultsSegmentReadable;
import io.engineblock.activityapi.output.Output;

import java.util.Collections;
import java.util.LinkedList;

/**
 * This will implement a result buffer that puts cycles in order when possible,
 * according to a sliding window.
 */
public class ReorderingConcurrentResultBuffer implements Output {

    private LinkedList<CycleResultsSegment> segments = new LinkedList<>();
    private Output downstream;
    private int maxCount = 5000;
    private final int threshold;
    private int currentCount;

    public ReorderingConcurrentResultBuffer(Output downstream, int maxCount, int threshold) {
        this.downstream = downstream;
        this.maxCount = maxCount;
        this.threshold = threshold;
    }

    @Override
    public boolean onCycleResult(long completedCycle, int result) {
        this.onCycleResultSegment(CycleResultsSegmentReadable.forCycleResult(completedCycle, result));
        return true;
    }

    @Override
    public synchronized void onCycleResultSegment(CycleResultsSegment segment) {
        segments.add(segment);
        currentCount+=segment.getCount();
        if (currentCount>=threshold) {
            Collections.sort(segments);
        }
        downstream.onCycleResultSegment(segments.removeFirst());
    }

    @Override
    public void close() throws Exception {
        for (CycleResultsSegment segment : segments) {
            downstream.onCycleResultSegment(segment);
        }
        downstream.close();

    }
}
