/*
*   Copyright 2015 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package com.metawiring.tools.activitycore.fortesting;

import com.metawiring.tools.activityapi.Input;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This cycle value supplier blocks the caller, only letting it complete
 * for each value that is set from the controlling producer. This is just
 * for testing. The convenience notify methods are to make tests more obvious.
 */
public class BlockingCycleValueSupplier implements Input {

    private final AtomicLong cycle = new AtomicLong(0L);

    public BlockingCycleValueSupplier setValue(long newFixedCycle) {
        cycle.set(newFixedCycle);
        return this;
    }

    @Override
    public long getMin() {
        return Long.MIN_VALUE;
    }

    @Override
    public long getMax() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getAsLong() {
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException("Unexpected interruption in synchronized test method.");
            }
        }
        return cycle.get();
    }

    public void setForSingleReader(long value) {
        setValue(value);
        synchronized (this) {
            this.notify();
        }
    }

    public void setForAllReaders(long value) {
        setValue(value);
        synchronized (this) {
            this.notifyAll();
        }
    }
}
