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

package io.engineblock.activityapi.cycletracking.markers.filebuffer;

import io.engineblock.activityapi.cycletracking.buffers.CycleResult;
import io.engineblock.activityapi.cycletracking.buffers.CycleResultRLETargetBuffer;
import io.engineblock.activityapi.cycletracking.buffers.CycleSegment;
import io.engineblock.activityapi.cycletracking.markers.Marker;
import io.engineblock.activityapi.cycletracking.markers.SegmentMarker;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@link Marker} that writes cycles and results to an RLE-based file format.
 *
 * This marker creates a file on disk and appends one or more (long,long,byte)
 * tuples to it as buffering extents are filled. This tuple format represents
 * the closed-open interval of cycles and the result associated with them.
 * The file is expected to contain only cycle ranges in order.
 *
 * <p>It <em>is</em> valid for RLE segments to be broken apart into contiguous
 * ranges. Any implementation should treat this as normal.
 */
public class FileBufferRLEMarker implements SegmentMarker {

    // For use in allocating file data, etc
    private final static Logger logger = LoggerFactory.getLogger(FileBufferRLEMarker.class);

    private MappedByteBuffer mbb;
    private RandomAccessFile file;
    private FileBufferConfig config;

    private CycleResultRLETargetBuffer targetBuffer = new CycleResultRLETargetBuffer();

    public FileBufferRLEMarker(ActivityDef activityDef) {
        config = new FileBufferConfig(activityDef);
    }


    @Override
    public void onCycleSegment(CycleSegment segment) {
        for (CycleResult cycleResult : segment) {
            boolean buffered = targetBuffer.onCycleResult(cycleResult);
            if (!buffered) {
                flush();
                targetBuffer = new CycleResultRLETargetBuffer(config.extentSize);
                boolean bufferedAfterFlush = targetBuffer.onCycleResult(cycleResult);
                if (!bufferedAfterFlush) {
                    throw new RuntimeException("Failed to record result in new target buffer");
                }
            }
        }
    }

    private void flush() {
        ByteBuffer nextFileExtent = targetBuffer.toByteBuffer();
        logger.trace("RLE result extent is " + nextFileExtent.remaining() + " bytes ("
                + (nextFileExtent.remaining() / CycleResultRLETargetBuffer.BYTES)
                + ") tuples");
        this.ensureCapacity((mbb == null ? 0 : mbb.capacity()) + nextFileExtent.remaining());
        mbb.put(nextFileExtent);
        logger.trace("extent appended");
    }

    @Override
    public void close() throws Exception {
        file.close();
    }


    private synchronized MappedByteBuffer ensureCapacity(long newCapacity) {
        try {
            logger.info("resizing marking file from " + (mbb == null ? 0 : mbb.capacity()) + " to " + newCapacity);
            if (file == null) {
                File filepath = new File(config.filename);
                if (Files.deleteIfExists(filepath.toPath())) {
                    logger.warn("removed extant file '" + config.filename + "'");
                }
                file = new RandomAccessFile(config.filename, "rw");
                file.seek(0);
            } else {
                long pos = mbb.position();
                file.setLength(newCapacity);
                file.seek(pos);
            }
            mbb = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, newCapacity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mbb;
    }

    private class FileBufferConfig {
        public final String filename; // Where the
        public final int extentSize; // logical chunksize boundary to fill to on overrun

        public FileBufferConfig(ActivityDef activityDef) {
            Optional<String> marker = activityDef.getParams().getOptionalString("marker");
            marker.orElseThrow(() -> new RuntimeException("marker parameter is missing?"));
            logger.debug("parsing marker config:" + marker.get());
            Map<String, String> params =
                    Arrays.stream(marker.get().split(",", 2)[1].split(","))
                            .map(s -> s.split("="))
                            .collect(Collectors.toMap(o -> o[0], o -> o[1]));
            this.filename = params.getOrDefault("file", activityDef.getAlias()) + ".rlemarkers";
            int suggested_extentsize = Integer.valueOf(params.getOrDefault("extentSize", String.valueOf(1024 * 1024)));
            extentSize = suggested_extentsize*CycleResultRLETargetBuffer.BYTES;
        }

    }


}
