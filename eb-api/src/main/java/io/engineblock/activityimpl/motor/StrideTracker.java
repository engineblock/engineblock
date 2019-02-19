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

package io.engineblock.activityimpl.motor;

import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.ops.fluent.opfacets.*;
import io.engineblock.activityapi.cyclelog.buffers.Buffer;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResult;
import io.engineblock.activityapi.output.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class StrideTracker<D> extends Buffer<CycleResult> implements OpEvents<D> {
    private final static Logger logger = LoggerFactory.getLogger(StrideTracker.class);

    private final Timer strideServiceTimer;
    private final Timer strideResponseTimer;

    private final OpImpl<Void> strideOp;
    private final Output output;

    public StrideTracker(Timer strideServiceTimer, Timer strideResponseTimer, long strideWaitTime, long initialCycle, int size, Output output) {
        super(CycleResult[].class, size);
        this.strideServiceTimer = strideServiceTimer;
        this.strideResponseTimer = strideResponseTimer;

        this.strideOp =new OpImpl<>();
        strideOp.setCycle(initialCycle);
        strideOp.setWaitTime(strideWaitTime);

        this.output = output;
    }

    /**
     * Each strideOp opTracker must be started before any ops that it tracks
     *
     */
    public void start() {
        this.strideOp.start();
    }

    @Override
    public void onOpStarted(StartedOp op) {
    }

    @Override
    public void onOpSuccess(SucceededOp op) {
        super.put(op);
    }

    @Override
    public void onOpFailure(FailedOp op) {
        super.put(op);
    }

    @Override
    public void onOpSkipped(SkippedOp<D> op) {
        super.put(op);
    }


    /**
     * When a stride is complete, do house keeping. This effectively means when N==stride ops have been
     * submitted to this buffer, which is tracked by {@link Buffer#put(Object)}.
     */
    public void onFull() {
        strideOp.succeed(0);
        logger.trace("completed strideOp with first result cycle (" + strideOp.getCycle() + ")");
        strideServiceTimer.update(strideOp.getResponseTimeNanos(), TimeUnit.NANOSECONDS);
        if (strideResponseTimer!=null) {
            strideResponseTimer.update(strideOp.getResponseTimeNanos(),TimeUnit.NANOSECONDS);
        }

        if (output != null) {
            try {
                flip();
                int remaining = remaining();
                for (int i = 0; i < remaining; i++) {
                    CycleResult opc = get();
                    output.onCycleResult(opc);
                }
            } catch (Exception t) {
                logger.error("Error while feeding cycle result to output '" + output + "', error:" + t);
                throw t;
            }
        }
    }

    @Override
    protected int compare(CycleResult one, CycleResult other) {
        return one.compareTo(other);
    }

}
