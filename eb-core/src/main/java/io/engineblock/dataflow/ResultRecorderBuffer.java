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

package io.engineblock.dataflow;

import io.engineblock.activityimpl.ResultRecorder;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Receive results from upstream processes, but only send them down
 * stream to the delegate result recorder when there are no gaps.
 */
public class ResultRecorderBuffer implements ResultRecorder {

    private final ResultRecorder target;
    private List<LongBuffer> bufferList = new ArrayList<>();
    private int windowSize;
    private long min, max;


    public ResultRecorderBuffer(ResultRecorder target, int windowSize) {
        this.target = target;
        this.windowSize = windowSize;
        addBuffer();
    }

    private void addBuffer() {
        LongBuffer buffer = LongBuffer.allocate(windowSize);
    }

    @Override
    public void recordResult(long offset, char result) {
        if

    }

    @Override
    public void close() throws IOException {

    }
}
