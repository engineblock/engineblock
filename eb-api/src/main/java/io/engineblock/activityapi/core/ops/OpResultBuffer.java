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

package io.engineblock.activityapi.core.ops;

import io.engineblock.activityapi.core.ops.fluent.opcontext.BaseOpContext;
import io.engineblock.activityapi.core.ops.fluent.opcontext.OpContext;
import io.engineblock.activityapi.cyclelog.buffers.Buffer;

/**
 * An OpResultBuffer
 */
public class OpResultBuffer extends Buffer<OpContext> implements OpContext.OpEvents {

    private OpContext context;

    public OpResultBuffer(long initialCycle, long waitTime, Class<OpContext[]> clazz, int size) {
        super(clazz, size);
        this.context = new BaseOpContext();
        context.setCycle(initialCycle);
        context.setWaitTime(waitTime);
    }

    @Override
    protected int compare(OpContext one, OpContext other) {
        return one.compareTo(other);
    }

    @Override
    public void onAfterOpStop(OpContext opc) {
        put(opc);
    }

    public OpContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return ((context!=null) ? "context:" + String.valueOf(context) : "") +
                "buffer: " + super.toString();
    }

}