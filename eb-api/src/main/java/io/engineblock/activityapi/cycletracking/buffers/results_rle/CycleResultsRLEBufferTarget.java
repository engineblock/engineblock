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
import io.engineblock.activityapi.output.Output;

import java.nio.ByteBuffer;

/**
 * Implements a convenient target buffer for Marker data that can be sued
 * to create nio ByteBuffers easily.
 *
 * This is not thread-safe. It is not meant to be used by concurrent callers.
 *
 * It is recommended to use the {@link AutoCloseable} method to ensure that
 * partial runs are flushed automatically. Access the buffer for read via either
 * the {@link #toByteBuffer()} or the {@link #toReadable()} methods will
 * automatically {@link #flush()} and invalidate the writable buffer, so further writes
 * will be deemed invalid and will cause an exception to be thrown.
 */
public class CycleResultsRLEBufferTarget implements Output {

    public final static int BYTES = Long.BYTES + Long.BYTES + Byte.BYTES;

    private ByteBuffer buf;
    private long lastCycle = Long.MIN_VALUE;
    private long lastResult = Integer.MIN_VALUE;
    private long runlength = 0L;
    private boolean flushed = false;

    /**
     * Create a buffer with the provided ByteBuffer.
     *
     * @param buf the source data
     */
    public CycleResultsRLEBufferTarget(ByteBuffer buf) {
        this.buf = buf;
    }

//    /**
//     * Create a buffer with an automatic capacity of around 1MiB.
//     */
//    public CycleResultsRLEBufferTarget() {
//        this(1024 * 1024);
//    }
//
    /**
     * Create a target RLE buffer for the specified getCount in memory,
     * rounded to the nearest record getCount.
     *
     * @param elementCount The number of elements to buffer.
     */
    public CycleResultsRLEBufferTarget(int elementCount) {
        this(ByteBuffer.allocate(elementCount * BYTES));
    }

    /**
     * Convert the contents of this RLE buffer to a readable and
     * invalide it for writing.
     * @return a CycleResultRLEBuffer
     */
    public CycleResultsRLEBufferReadable toReadable() {
        flush();
        ByteBuffer readable = buf.duplicate();
        readable.flip();
        return new CycleResultsRLEBufferReadable(readable);
    }

    public ByteBuffer toByteBuffer() {
        flush();
        ByteBuffer bb = buf.duplicate();
        bb.flip();
        return bb;
    }

    /**
     * Record new cycle result data in the buffer, and optionally flush any
     * completed RLE segments to the internal ByteBuffer.
     *
     * @param cycle The cycle number being marked.
     * @param result the result ordinal
     *
     * @throws RuntimeException if the buffer has been converted to a readable form
     * @return false if there was no more room in the buffer for another tuple, true otherwise.
     */
    @Override
    public boolean onCycleResult(long cycle, int result) {
        if (buf == null) {
            throw new RuntimeException("Attempt to append a buffer that is active for reading with cycle:" + cycle + " result:" +result);
        }
        if (buf.remaining()<BYTES) {
            return false;
        }
        if (cycle != lastCycle + 1 || lastResult != result) {
            if (lastCycle != Long.MIN_VALUE) {
                checkpoint(lastCycle + 1 - runlength, lastCycle + 1, lastResult);
            }
        }

        lastCycle = cycle;
        lastResult = result;
        runlength++;
        flushed = false;
        return true;
    }

    private void checkpoint(long istart, long iend, long lastResult) {
        if (lastResult > Byte.MAX_VALUE) {
            throw new RuntimeException("Unable to encode result values greater than Byte.MAX_VALUE.");
        }
        if (lastCycle<0) {
            throw new RuntimeException("Unable to encode cycle values less than 0");
        }
        buf.putLong(istart).putLong(iend).put((byte) lastResult);
        runlength = 0;
    }

    public int getBufferCapacity() {
        return buf.capacity();
    }

    /**
     * Flushes any partial data that was submitted (an incomplete run of results,
     * for example), to the internal ByteBuffer, and marks flushed status.
     *
     * @return the getCount of the current buffer, in bytes.
     */
    private int flush() {
        if (!flushed) {
            checkpoint(lastCycle + 1 - runlength, lastCycle + 1, lastResult);
            flushed = true;
        }
        return buf.position();
    }

    @Override
    public void close() {
        flush();
    }

    public boolean onCycleResult(CycleResult cycleResult) {
        return this.onCycleResult(cycleResult.getCycle(),cycleResult.getResult());
    }


}
