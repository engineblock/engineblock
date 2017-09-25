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

import io.engineblock.activityapi.cycletracking.CycleResultSink;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileBufferResultSink implements CycleResultSink {
    private final static Logger logger = LoggerFactory.getLogger(FileBufferResultSink.class);
    private MappedByteBuffer mbb;
    private RandomAccessFile file;
    private FileBufferConfig config;
    private long currentSize=0;

    public FileBufferResultSink(ActivityDef activityDef) {
        config = new FileBufferConfig(activityDef);
        ensureCapacity(config.size);
    }

    private synchronized MappedByteBuffer ensureCapacity(long capacity) {
        if (currentSize>capacity) {
            return mbb;
        }
        long blockCount1M=1L + capacity/(1024*1024);
        long newsize=(1024*1024)*blockCount1M;
        try {
            logger.info("resizing marking file from " + currentSize + " to " + newsize + ", for offset " + capacity);
            if (file==null) {
                file = new RandomAccessFile(config.filename,"rw");
                file.seek(0);
                for (int i = 0; i < newsize; i++) {
                    file.write(-1);
                }
                logger.info("padded new file with default -1 values");
            } else {
                file.setLength(newsize);
                file.seek(currentSize);
                for (long i=currentSize; i< newsize ; i++) {
                    file.write(-1);
                }
                logger.info("padded new file section with default -1 values");
            }
            currentSize=file.length();
            mbb=file.getChannel().map(FileChannel.MapMode.READ_WRITE,0,currentSize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mbb;
    }


    @Override
    public boolean consumeResult(long completedCycle, int result) {
        if (completedCycle>=currentSize) {
            synchronized (this) {
                mbb=ensureCapacity(completedCycle);
            }
        }
        try {
            mbb.put((int) completedCycle, (byte) (result & 127));
//        System.out.println("cycle="+completedCycle+",result="+result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private class FileBufferConfig {
        public final String filename; // Where the
        public final long size; // The initial size
        public final long growsize; // logical chunksize boundary to fill to on overrun

        public FileBufferConfig(ActivityDef activityDef) {
            Optional<String> marker = activityDef.getParams().getOptionalString("tracker");
            marker.orElseThrow(() -> new RuntimeException("marker parameter is missing?"));
            logger.debug("parsing marker config:" +marker.get());
            Map<String, String> params =
                    Arrays.stream(marker.get().split(",",2)[1].split(","))
                            .map(s -> s.split("="))
                            .collect(Collectors.toMap(o -> o[0], o -> o[1]));
            this.filename = params.getOrDefault("filename", activityDef.getAlias() + "-buffer.tracker");
            this.size = Long.valueOf(params.getOrDefault("size","1024"));
            this.growsize= Long.valueOf(params.getOrDefault("growsize",String.valueOf(1024*1024)));
        }

    }
}
