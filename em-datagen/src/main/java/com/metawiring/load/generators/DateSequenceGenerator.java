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

import com.metawiring.load.generator.FastForwardableGenerator;
import com.metawiring.load.generator.ThreadsafeGenerator;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class DateSequenceGenerator implements FastForwardableGenerator<Date>,ThreadsafeGenerator {

    private AtomicLong seq = new AtomicLong(0l);
    private long increment = 1l;

    public DateSequenceGenerator() {}
    public DateSequenceGenerator(int increment) {
        this.increment=increment;
    }
    public DateSequenceGenerator(String increment) { this.increment = Integer.valueOf(increment); }

    @Override
    public Date get() {
        long setval = seq.addAndGet(increment);
        return new Date(setval);
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}
