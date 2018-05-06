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

import io.engineblock.activityapi.cyclelog.buffers.Buffer;

public class OpResultBuffer<T> extends Buffer<OpContext> implements OpContext.Sink {

    private T context;
    private final Sink<T> sink;

    public OpResultBuffer(T context, Sink<T> sink, Class<OpContext[]> clazz, int size) {
        super(clazz, size);
        this.context = context;
        this.sink = sink;
    }

    public OpResultBuffer(Sink<T> sink, Class<OpContext[]> clazz, int size) {
        super(clazz, size);
        this.sink = sink;
    }

    @Override
    protected int compare(OpContext one, OpContext other) {
        return one.compareTo(other);
    }

    @Override
    protected void onFull() {
        if (sink!=null) {
            this.flip();
            sink.handle(this);
        }
    }

    @Override
    public void handle(OpContext opContext) {
        put(opContext);
    }

    public T getContext() {
        return context;
    }

    @Override
    public String toString() {
        return ((context!=null) ? "context:" + String.valueOf(context) : "") +
                ((sink!=null) ? " sink:" + String.valueOf(sink) :"") +
                "buffer: " + super.toString();
    }

    public static interface Sink<T> {
        void handle(OpResultBuffer<T> opContext);
    }


}