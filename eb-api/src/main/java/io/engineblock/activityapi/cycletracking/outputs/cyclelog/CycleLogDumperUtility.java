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

package io.engineblock.activityapi.cycletracking.outputs.cyclelog;

import io.engineblock.activityapi.cycletracking.buffers.results.CycleResult;
import io.engineblock.activityapi.cycletracking.buffers.results.CycleResultsSegment;
import io.engineblock.activityapi.cycletracking.buffers.results_rle.CycleResultsRLEBufferReadable;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class CycleLogDumperUtility {

    public static void main(String[] args) {
        if (args.length==0) {
            System.out.println("USAGE: CyclesCLI <filename>");
        }
        String filename = args[0];
        new CycleLogDumperUtility().dumpData(filename);
    }

    private void dumpData(String filename) {
        File filepath = new File(filename);
        MappedByteBuffer mbb=null;
        if (!filepath.exists()) {
            throw new RuntimeException("file path '" + filename + "' does not exist!");
        }
        try {
            RandomAccessFile raf = new RandomAccessFile(filepath, "rw");
            mbb = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int readsize=100;
        while (mbb.remaining()>0) {
            CycleResultsRLEBufferReadable readable = new CycleResultsRLEBufferReadable(readsize, mbb);
            CycleResultsSegment segment = readable.getCycleResultsSegment(1);
            for (CycleResult cycleResult : segment) {
                System.out.println(cycleResult.getCycle() + ": " + cycleResult.getResult());
            }
        }

    }
}
