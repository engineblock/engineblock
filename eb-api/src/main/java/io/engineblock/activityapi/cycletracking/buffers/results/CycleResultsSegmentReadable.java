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

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Implements a cycle result segment in a basic buffer
 * that contains the cycle and the result in long, byte format.
 */
public class CycleResultsSegmentReadable implements CycleResultsSegment {

    private final ByteBuffer buf;
    private final static int BYTES = Long.BYTES + Byte.BYTES;

    public CycleResultsSegmentReadable(ByteBuffer buf) {
        this.buf = buf;
    }

    @NotNull
    @Override
    public Iterator<CycleResult> iterator() {
        return new Iter();
    }

    public static CycleResultsSegment forCycleResult(long completedCycle, int result) {
        ByteBuffer single = ByteBuffer.allocate(BYTES);
        single.putLong(completedCycle).put((byte) result);
        single.flip();
        return new CycleResultsSegmentReadable(single);
    }

    @Override
    public int getCount() {
        return buf.position()/BYTES;
    }

    @Override
    public long getMinCycle() {
        if (buf!=null && buf.limit()>0) {
            return buf.getLong(0);
        }
        return Long.MIN_VALUE;
    }

    private class Iter implements Iterator<CycleResult> {
        private int offset=-BYTES;

        @Override
        public boolean hasNext() {
            return (offset+ BYTES <buf.limit());
        }

        @Override
        public CycleResult next() {
            offset+= BYTES;
            return new BBCycleResult(offset);
        }
    }

    private class BBCycleResult implements CycleResult {

        private int offset;

        BBCycleResult(int offset) {
            this.offset=offset;
        }

        @Override
        public long getCycle() {
            return buf.getLong(offset);
        }

        @Override
        public int getResult() {
            return buf.get(offset+Long.BYTES);
        }
    }

}
