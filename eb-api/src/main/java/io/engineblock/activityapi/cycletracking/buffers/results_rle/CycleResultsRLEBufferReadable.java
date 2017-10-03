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

import io.engineblock.activityapi.cycletracking.buffers.CycleResultSegmentsReadable;
import io.engineblock.activityapi.cycletracking.buffers.results.CycleResultsSegment;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Implements a cycle result segment in a run-length encoded buffer
 * that contains the cycle interval and the result in long, long, byte format,
 * where the last value (the second long value) is *not* included in the
 * cycle inteval. (closed-open interval)
 * <p>This is <em>not</em> a threadsafe iterator. It references buffer data
 * that is presumed to be access by only one reader for the sake of efficiency.
 */
public class CycleResultsRLEBufferReadable implements CycleResultSegmentsReadable {

    public final static int BYTES = Long.BYTES + Long.BYTES + Byte.BYTES;
    private final ByteBuffer buf;

    public CycleResultsRLEBufferReadable(ByteBuffer buf) {
        this.buf = buf;
    }

    public CycleResultsRLEBufferReadable(int readSizeInSpans, ByteBuffer src) {
        readSizeInSpans = readSizeInSpans * BYTES;
        int bufsize = Math.min(readSizeInSpans, src.remaining());
        byte[] bbuf = new byte[bufsize];
        src.get(bbuf);
        this.buf = ByteBuffer.wrap(bbuf);
    }

    @NotNull
    @Override
    public Iterator<CycleResultsSegment> iterator() {
        return new ResultSpanIterator(buf);
    }

    private class ResultSpanIterator implements Iterator<CycleResultsSegment> {
        private final ByteBuffer iterbuf;

        public ResultSpanIterator(ByteBuffer buf) {
            this.iterbuf = buf;
        }

        @Override
        public boolean hasNext() {
            return iterbuf.remaining()>0;
        }

        @Override
        public CycleResultsSegment next() {
            long min = iterbuf.getLong();
            long nextMin = iterbuf.getLong();
            int result = iterbuf.get();
            return new CycleSpanResults(min, nextMin, result);
        }

        public String toString() {
            return "ResultSpanIterator (" + iterbuf.toString() + ")";
        }

    }

}
