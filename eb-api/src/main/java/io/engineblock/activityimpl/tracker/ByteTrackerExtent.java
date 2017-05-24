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
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple bytebuffer marker implementation
 */
public class ByteTrackerExtent implements SegmentedInput {

    private final long min; // The maximum value to be dispatched
    private final long max; // The minimum value to be dispatched
    private final AtomicInteger totalMarked; // The total number of marked values
    private final AtomicInteger totalServed; // the total number of served values
    byte[] markerData;
    private int size; // max-min
    private AtomicReference<ByteTrackerExtent> nextExtent = new AtomicReference<>();
    private boolean filled = false;

    /**
     * Create a simple marker extent
     *
     * @param min the first logical cycle to be returned by this tracker
     * @param max the last logical cycle to be returned by this tracker
     */
    public ByteTrackerExtent(long min, long max) {
        this.min = min;
        this.max = max;
        this.size = (int) (max - min) + 1;
        markerData = new byte[size];
        totalMarked = new AtomicInteger(0);
        totalServed = new AtomicInteger(0);
//        maxcont = new AtomicLong(min - 1);
//        currentValue = new AtomicLong(min);
    }

    /**
     * mark the named cycle in the extent, or in any future extent that we know. The return value determines the
     * known state of the extent:
     * <ol>
     * <li>negative value: indicates an attempt to mark a value outside the range,
     * either before min or after max of the furthest known extent</li>
     * <li>zero: indicates successful marking, but exactly no remaining space available.
     * This is how a marking thread can detect that it was the one that finished marking
     * an extent.</li>
     * <li>positive value: indicates how many cycles remain available in the extent to
     * be marked.</li>
     * </ol>
     *
     * @param cycle  The cycle to be marked
     * @param result the result code to mark in the cycle
     * @return the number of cycles remaining after marking, or a negative number indicating an error.
     */
    public long markResult(long cycle, int result) {
        if (cycle < min) {
            return cycle - min; // how short were we? ( a negative number )
        }
        if (cycle > max) {
            ByteTrackerExtent next = this.nextExtent.get();
            if (next != null) {
                return next.markResult(cycle, result);
            } else {
                return max - cycle; // how long were we? ( a negative number )
            }
        }

        int position = (int) (cycle - min);
        byte resultCode = (byte) (result & 127);
        markerData[position] = resultCode;
        int i = totalMarked.incrementAndGet();

        return size-i;
    }

    @Override
    public CycleSegment getSegment(int stride) {
        if (!filled) {
            filled = isFullyFilled();
            if (!filled) {
                throw new RuntimeException("Not allowed to read segments on unfilled extent.");
            }
        }

        while (totalServed.get() < size) {
            int current = totalServed.get();
            int next = current + stride;
            if (next <= totalMarked.get()) {
                if (totalServed.compareAndSet(current, next)) {
                    return CycleSegment.forData(current + min, markerData, current, stride);
                }
            }
        }
        return null;
    }
    // TODO: consider making intervals overlap perfectly with ...

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(min).append(",").append(max).append("): ")
                .append(", size=").append(this.size)
                .append(", marked=").append(this.totalMarked.get())
                .append(", served=").append(this.totalServed.get());
        if (markerData.length<1024*50) {
            sb.append(" data=");
            for (int i = 0; i < this.markerData.length; i++) {
                sb.append(markerData[i]).append(",");
            }

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

    /**
     * Find the last known extent, and add another after it, account for
     * contiguous ranges and extent size. Note that this does not mean
     * necessarily that the extent will be added immediately after the current one.
     *
     * @return The new extent that was created.
     */
    public ByteTrackerExtent extend() {

        ByteTrackerExtent lastExtent = this;
        while (lastExtent.getNextExtent().get() != null) {
            lastExtent = lastExtent.getNextExtent().get();
        }

        ByteTrackerExtent newLastExtent = new ByteTrackerExtent(
                lastExtent.getMin() + size, lastExtent.getMax() + size
        );

        if (!lastExtent.getNextExtent().compareAndSet(null, newLastExtent)) {
            throw new RuntimeException("There should be no contention for extending the extents");
        }

        return newLastExtent;
    }

    public long getMax() {
        return max;
    }

    public long getMin() {
        return min;
    }
}
