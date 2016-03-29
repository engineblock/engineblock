/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;
import org.apache.commons.math3.random.MersenneTwister;

public class RandomStringRangeGenerator implements Generator<String> {
    private MersenneTwister theTwister = new MersenneTwister(System.nanoTime());
    private long min;
    private long max;
    private long length;

    public RandomStringRangeGenerator(long min, long max) {
        this.min = min;
        this.max = max;
        this.length = max - min;
    }

    public RandomStringRangeGenerator(String min, String max) {
        this(Long.valueOf(min), Long.valueOf(max));
    }

    @Override
    public String get() {
        long value = Math.abs(theTwister.nextLong());
        value %= length;
        value += min;
        return String.valueOf(value);
    }

    public String toString() {
        return getClass().getSimpleName() + ":" + min + ":" + max;
    }
}
