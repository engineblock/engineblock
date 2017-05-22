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

package io.engineblock.activityimpl.tracker;

import io.engineblock.activityapi.cycletracking.CycleSegment;
import io.engineblock.activityapi.cycletracking.SegmentedInput;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple bytebuffer marker implementation
 */
public class ByteTrackerExtent implements SegmentedInput {

    private final AtomicLong min; // The maximum value to be dispatched
    private final AtomicLong max; // The minimum value to be dispatched
    private final AtomicInteger totalMarked; // The total number of marked values
    private final AtomicInteger totalServed; // the total number of served values
//    private final AtomicLong maxcont; // max contiguously marked value, starting from min-1
//    private final AtomicLong currentValue; // The next value that would be dispatched
    private int size; // max-min
    byte[] markerData;
    private AtomicReference<ByteTrackerExtent> nextExtent=new AtomicReference<>();

    /**
     * Create a simple marker extent
     *
     * @param min the first logical cycle to be returned by this tracker
     * @param max the last logical cycle to be returned by this tracker
     */
    public ByteTrackerExtent(long min, long max) {
        this.min = new AtomicLong(min);
        this.max = new AtomicLong(max);
        this.size = (int)(max - min)+1;
        markerData = new byte[size];
        totalMarked = new AtomicInteger(0);
        totalServed = new AtomicInteger(0);
//        maxcont = new AtomicLong(min - 1);
//        currentValue = new AtomicLong(min);
    }

    public boolean markResult(long cycle, int result) {
        if (cycle < min.get() || cycle > max.get()) {
            return false;
        }

        int position = (int) (cycle - min.get());
        byte resultCode = (byte) (result & 127);
        markerData[position]=resultCode;
        totalMarked.incrementAndGet();

        return true;
    }

    @Override
    public CycleSegment getSegment(int stride) {
        while (true) {
            int current = totalServed.get();
            int next=current+stride;
            if (next<=totalMarked.get()) {
                if (totalServed.compareAndSet(current,next)) {
                    return CycleSegment.forData(next,markerData,current,stride);
                }
            } else if (next<=max.get()) {
                try {
                    Thread.sleep(0L,1000);
                } catch (InterruptedException ignored) {
                }
            } else {
                throw new RuntimeException("overread on " + this + " for " + stride + " cycles");
            }
        }
    }

    public AtomicLong getMaxCycle() {
        return max;
    }

    public AtomicLong getMinCycle() {
        return min;
    }

    // TODO: consider making intervals overlap perfectly with ...

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(min.get()).append(",").append(max.get()).append("): ")
                .append(", size=").append(this.size);
        sb.append(" data=");
        for (int i = 0; i < this.markerData.length; i++) {
            sb.append(markerData[i]).append(",");
        }
        return sb.toString();
    }

    public boolean isFullyFilled() {
        return (totalMarked.get() == size);
    }
    public boolean isFullyServed() {
        return (totalServed.get() == size);
    }


    public AtomicReference<ByteTrackerExtent> getNextExtent() {
        return nextExtent;
    }

    public void setNextExtent(AtomicReference<ByteTrackerExtent> nextExtent) {
        this.nextExtent = nextExtent;
    }

    // For testing
    byte[] getMarkerData() {
        return markerData;
    }
}
