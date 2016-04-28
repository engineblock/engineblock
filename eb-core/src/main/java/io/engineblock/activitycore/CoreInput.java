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
package io.engineblock.activitycore;

import io.engineblock.activityapi.Input;

import java.security.InvalidParameterException;
import java.util.concurrent.atomic.AtomicLong;

public class CoreInput implements Input {

    private AtomicLong min = new AtomicLong(0L);
    private AtomicLong max = new AtomicLong(Long.MAX_VALUE);

    public CoreInput setRange(long min, long max) {
        if (min>max) {
            throw new InvalidParameterException("min (" + min + ") must be less than or equal to max (" + max + ")");
        }
        this.min.set(min);
        this.max.set(max);
        setNextValue(min);
        return this;
    }

    private final AtomicLong cycleValue = new AtomicLong(0L);

    public CoreInput setNextValue(long newValue) {
        if (newValue < min.get() || newValue > max.get()) {
            throw new RuntimeException(
                    "new value (" + newValue + ") must be within min..max range: [" + min + ".." + max + "]"
            );
        }
        cycleValue.set(newValue);
        return this;
    }

    @Override
    public long getAsLong() {
        return cycleValue.getAndIncrement();
    }

    @Override
    public long getMin() {
        return min.get();
    }

    @Override
    public long getMax() {
        return max.get();
    }
}
