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

package io.engineblock.activityapi.cycletracking.buffers.results;

import java.nio.ByteBuffer;

/**
 * Implements a cycle result segment in a basic buffer
 * that contains the cycle and the result in long, byte format.
 * This is not thread safe.
 */
public class CycleResultSegmentBuffer {

    private final ByteBuffer buf;
    private final static int BYTES = Long.BYTES + Byte.BYTES;

    public CycleResultSegmentBuffer(int resultCount) {
        this.buf = ByteBuffer.allocate(resultCount*BYTES);
    }

    public void update(long cycle, int result) {
        buf.putLong(cycle).put((byte) result);
    }

    public void update(CycleResult result) {
        buf.putLong(result.getCycle()).put((byte) result.getResult());
    }

    public CycleResultsSegment toReader() {
        buf.flip();
        CycleResultsSegmentReadable readable = new CycleResultsSegmentReadable(buf);
        return readable;
    }

}
