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

package io.engineblock.activityapi.core.ops.fluent.opcontext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class BaseOpContext<D> implements OpContext<D> {

    private final static OpEvents NULLSINK = new NullOpEvents();
    private static AtomicLong idgen = new AtomicLong(0L);
    public final long ctxid = idgen.getAndIncrement();
    private long usages = 0L;

    private long delayNanos;
    private long startedAtNanos = 0L;
    private long endedAtNanos = 0L;

    private long cycle;
    private int result;

    private int tries=0;
    private List<OpEvents> opEvents = new ArrayList<OpEvents>();
    private D data;

    public BaseOpContext() {
    }

    @Override
    public OpContext reset() {
        for (OpEvents opEvents : this.opEvents) {
            opEvents.onOpReset(this);
        }
        startedAtNanos=0L;
        delayNanos=0L;
        endedAtNanos=0L;
        cycle=0L;
        result=0;
        opEvents =null;

        opEvents.clear();
        return this;
    }

    @Override
    public OpContext addSink(OpEvents opEvents) {
        this.opEvents.add(opEvents);
        return this;
    }

    @Override
    public OpContext setWaitTime(long delayNanos) {
        this.endedAtNanos = Long.MIN_VALUE;
        this.cycle = cycle;
        this.delayNanos = delayNanos;
        this.startedAtNanos = System.nanoTime();
        usages++;
        return this;
    }

    @Override
    public OpContext start() {
        this.endedAtNanos = Long.MIN_VALUE;
        this.startedAtNanos = System.nanoTime();
        tries=1;
        usages++;
        for (OpEvents opEvents : this.opEvents) {
            opEvents.onOpStart(this);
        }
        return this;
    }

    public OpContext retry() {
        this.startedAtNanos = System.nanoTime();
        this.endedAtNanos = Long.MIN_VALUE;
        usages++;
        tries++;
        for (OpEvents opEvents : this.opEvents) {
            opEvents.onOpRestart(this);
        }
        return this;
    }

    @Override
    public OpContext stop(int result) {
        this.endedAtNanos = System.nanoTime();
        this.result = result;
        for (OpEvents opEvents : this.opEvents) {
            opEvents.onAfterOpStop(this);
        }
        synchronized(this) {
            notifyAll();
        }
        return this;
    }

    @Override
    public long getFinalServiceTime() {
        return (endedAtNanos - startedAtNanos);
    }

    @Override
    public long getCumulativeServiceTime() {
        return System.nanoTime() - startedAtNanos;
    }

    @Override
    public long getFinalResponseTime() {
        return delayNanos + (endedAtNanos - startedAtNanos);
    }

    @Override
    public long getCumulativeResponseTime() {
        return delayNanos + (System.nanoTime() - startedAtNanos);
    }

    @Override
    public long getWaitTime() {
        return delayNanos;
    }

    @Override
    public int getResult() {
        return result;
    }

    @Override
    public int getTries() {
        return tries;
    }

    @Override
    public long getCycle() {
        return cycle;
    }

    @Override
    public long getCtxId() {
        return ctxid;
    }

    @Override
    public D getData() {
        return data;
    }

    @Override
    public OpContext<D> setData(D data) {
        return this;
    }

    @Override
    public boolean isRunning() {
        return endedAtNanos < 0L;
    }

    @Override
    public String toString() {
        return "BasicOpContext{" +
                "(S-state,I-id,C-cycle,T-tries,U-use)=(S-" +
                (startedAtNanos<=0 ? "RESET" : (endedAtNanos<=0 ? "RUNNING" : "STOPPED") ) +
                ",I-" + ctxid +
                ",C-" + cycle +
                ",T-" + tries +
                ",U-" + usages +
                ")" +
                ", result=" + result +
                ", delayNanos=" + delayNanos +
                ", startedAtNanos=" + startedAtNanos +
                ", endedAtNanos=" + endedAtNanos +
                ", usages=" + usages +
                ", opEvents=" + opEvents.size() +
                '}';
    }

    @Override
    public void setCycle(long cycle) {
        this.cycle = cycle;
    }

    private final static class NullOpEvents implements OpEvents {
    }

    protected long getStartedAtNanos() {
        return startedAtNanos;
    }

}
