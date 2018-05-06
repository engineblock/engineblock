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

package io.engineblock.activityapi.core;

import io.engineblock.activityapi.cyclelog.buffers.results.CycleResult;

public class OpContext implements CycleResult {

    private final long cycle;
    private final Sink sink;
    private int result;
    private final long delayNanos;
    private long startedAtNanos;
    private long endedAtNanos;

    public OpContext(Sink sink, long cycle, long delayNanos, long staredAtNanos) {
        this.sink = sink;
        this.cycle = cycle;
        this.delayNanos = delayNanos;
        this.startedAtNanos = staredAtNanos;

    }
    public OpContext(Sink sink, long cycle, long delayNanos) {
        this.sink = sink;
        this.cycle = cycle;
        this.delayNanos = delayNanos;
        this.startedAtNanos = System.nanoTime();
    }

    public void setResult(int result) {
        this.endedAtNanos=System.nanoTime();
        this.result = result;
        this.sink.handle(this);
    }

    public void stop() {
        this.endedAtNanos=System.nanoTime();
    }


    public long getServiceTime() {
        return (endedAtNanos - startedAtNanos);
    }

    public long getTotalLatency() {
        return delayNanos + (endedAtNanos - startedAtNanos);
    }

    public int getResult() {
        return result;
    }

    public long getCycle() {
        return cycle;
    }


    public static interface Sink {
        void handle(OpContext opContext);
    }

}
