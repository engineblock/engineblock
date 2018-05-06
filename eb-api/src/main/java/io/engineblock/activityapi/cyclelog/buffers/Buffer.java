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

package io.engineblock.activityapi.cyclelog.buffers;

import java.lang.reflect.Array;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public abstract class Buffer<T> implements Comparable<Buffer<T>> {

    private int position;
    private int limit;
    private T[] data;

    public Buffer(Class<T[]> clazz,int size) {
        data = clazz.cast(Array.newInstance(clazz.getComponentType(), size));
        limit=data.length;
        position=0;
    }

    protected void onFull() {
    }

    protected abstract int compare(T one, T other);

    public int position() {
        return position;
    }

    public Buffer<T> position(int position) {
        this.position=position;
        return this;
    }

    public int remaining() {
        return limit-position;
    }

    public Buffer<T> put(T element) {
        if (position>=limit) {
            throw new BufferOverflowException();
        }
        data[position++]=element;
        if (position==limit) {
            onFull();
        }
        return this;
    }

    public T get() {
        if (position >=limit) {
            throw new BufferUnderflowException();
        }
        T got = data[position++];
        return got;
    }


    public Buffer<T> flip() {
        this.limit=position;
        this.position=0;
        return this;
    }

    @Override
    public int compareTo(Buffer<T> other) {
        int lengthDiff = Integer.compare(data.length, other.data.length);
        if (lengthDiff!=0) {
            return lengthDiff;
        }
        int compareTo = Math.min(position, other.position);

        for (int pos = 0; pos<compareTo; pos++) {
            int diff = compare(data[pos],other.data[pos]);
            if (diff!=0) {
                return diff;
            }
        }
        return 0;
    }


    @Override
    public String toString() {
        return "position=" + this.position + ", limit=" + this.limit + ", capacity=" + (data!=null ? data.length : "NULLDATA");
    }
}
