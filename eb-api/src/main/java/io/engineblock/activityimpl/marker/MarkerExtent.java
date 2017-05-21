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

package io.engineblock.activityimpl.marker;

import io.engineblock.activityapi.cycletracking.CycleMarker;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple bytebuffer marker implementation
 */
public class MarkerExtent implements CycleMarker {

    private ByteBuffer markerData;
    private long size;
    private final long start;
    private AtomicLong completed = new AtomicLong(-1L);

    /**

     * Create a simple marker extent
     * @param start the first logical cycle in this marker's state
     * @param size The number of cycles to be tracked in this extent.
     */
    public MarkerExtent(long start, long size) {
        this.start = start;
        this.size = size;
        markerData=ByteBuffer.allocate((int)size);
    }

    @Override
    public void markResult(long completedCycle, int result) {
        markerData.put((int) (completedCycle- start), (byte) result);
    }
    public ByteBuffer getMarkerData() {
        return markerData;
    }

    public void setMarkerData(ByteBuffer markerData) {
        this.markerData = markerData;
    }

    public long getStart() {
        return start;
    }

    public long getSize() {
        return size;
    }


}
