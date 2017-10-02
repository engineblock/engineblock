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
import io.engineblock.activityapi.cycletracking.buffers.results.CycleResult;
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
    private Iterator<CycleResult> it;

    public CycleResultsRLEBufferReadable(ByteBuffer buf) {
        this.buf = buf;
    }

    public CycleResultsRLEBufferReadable(int readsize, ByteBuffer src) {
        readsize = (readsize / BYTES) * BYTES;
        int bufsize = Math.min(readsize, src.remaining());
        byte[] bbuf = new byte[bufsize];
        src.get(bbuf);
        this.buf = ByteBuffer.wrap(bbuf);
    }

    public static CycleResultsRLEBufferReadable forOneRleSpan(ByteBuffer src) {
        if (src.remaining() >= BYTES) {
            byte[] segbuf = new byte[BYTES];
            src.get(segbuf);
            return new CycleResultsRLEBufferReadable(ByteBuffer.wrap(segbuf));
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public Iterator<CycleResultsSegment> iterator() {
        return new SegmentIterator(buf.duplicate());
    }

    private static class SegmentIterator implements Iterator<CycleResultsSegment> {

        private ByteBuffer sibuf;

        private SegmentIterator(ByteBuffer sibuf) {
            this.sibuf = sibuf;
        }

        @Override
        public boolean hasNext() {
            return sibuf.remaining()>0;
        }

        @Override
        public CycleResultsSegment next() {
            if (sibuf.remaining()<=0) {
                return null;
            }
            long min = sibuf.getLong();
            long nextMin =sibuf.getLong();
            int result = sibuf.get();
            return new CycleSpanResults(min,nextMin,result);
        }
    }

//    @Override
//    public CycleResultsSegment getCycleResultsSegment(int stride) {
//        if (it==null) {
//            it = this.getCycleResultIterable().iterator();
//        }
//        CycleResultSegmentBuffer resultBuffer = new CycleResultSegmentBuffer(stride);
//        for (int i = 0; i < stride; i++) {
//            if (it.hasNext()) {
//                CycleResult next = it.next();
//                resultBuffer.append(next);
//            } else {
//                it = null;
//                return null;
//            }
//        }
//        return resultBuffer.toReader();
//    }

//    public @NotNull Iterable<CycleResult> cycleResultIterable() {
//        return new Iterable<CycleResult>() {
//            @NotNull
//            @Override
//            public Iterator<CycleResult> iterator() {
//                return new ResultIterator(buf.duplicate());
//            }
//        };
//    }
//
//    private class ResultIterator implements Iterator<CycleResult> {
//
//        private final ByteBuffer iterbuf;
//        private long rleNextCycle = 0;
//        private long rleMaxCycleLimit = 0;
//        private int result;
//
//
//        public ResultIterator(ByteBuffer iterbuf) {
//            this.iterbuf = iterbuf;
//        }
//
//        @Override
//        public boolean hasNext() {
//            return ((rleNextCycle < rleMaxCycleLimit) || (iterbuf.hasRemaining()));
//        }
//
//        @Override
//        public CycleResult next() {
//            if (rleNextCycle >= rleMaxCycleLimit) {
//                rleNextCycle = iterbuf.getLong();
//                rleMaxCycleLimit = iterbuf.getLong();
//                result = iterbuf.get();
//            }
//            return new BasicCycleResult(rleNextCycle++, result);
//        }
//    }

    private class BasicCycleResult implements CycleResult {

        private final int result;
        private final long cycle;

        private BasicCycleResult(long cycle, int result) {
            this.result = result;
            this.cycle = cycle;
        }

        @Override
        public long getCycle() {
            return this.cycle;
        }

        @Override
        public int getResult() {
            return this.result;
        }
    }

}
