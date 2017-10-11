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

package io.engineblock.activityapi.cyclelog.outputs;

import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultArray;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultsSegment;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultsSegmentReadable;
import io.engineblock.activityapi.output.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;

/**
 * This will implement a result buffer that puts cycles in order when possible,
 * according to a sliding window.
 */
public class ReorderingConcurrentResultBuffer implements Output {

    private final static Logger logger = LoggerFactory.getLogger(ReorderingConcurrentResultBuffer.class);

    private LinkedList<CycleResultsSegment> segments = new LinkedList<>();
    private Output downstream;
    private final int threshold;
    private int currentCount;

    public ReorderingConcurrentResultBuffer(Output downstream, int threshold) {
        this.downstream = downstream;
        this.threshold = threshold;
    }

    @Override
    public synchronized boolean onCycleResult(long completedCycle, int result) {
        this.onCycleResultSegment(CycleResultsSegmentReadable.forCycleResult(completedCycle, result));
        return true;
    }

    @Override
    public synchronized void onCycleResultSegment(CycleResultsSegment segment) {
        if (!(segment instanceof CanSortCycles)) {
            segment = new CycleResultArray(segment);
        }
        ((CanSortCycles)segment).sort();
        segments.add(segment);
        currentCount+=segment.getCount();
        if (currentCount>=threshold) {
            logger.trace("Reordering threshold met: " + currentCount +"/" + threshold + ", sorting and pushing. (" + segments.size() + " segments)");
            Collections.sort(segments);
            while(currentCount>=threshold) {
                CycleResultsSegment head = segments.removeFirst();
                currentCount-=head.getCount();
                downstream.onCycleResultSegment(head);
            }
        }
    }

    @Override
    public synchronized void close() throws Exception {
        logger.trace("closing and flushing " + segments.size() + " segments");
        for (CycleResultsSegment segment : segments) {
            downstream.onCycleResultSegment(segment);
        }
        downstream.close();

    }
}
