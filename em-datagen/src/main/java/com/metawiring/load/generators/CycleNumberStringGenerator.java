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
import org.joda.time.format.DateTimeFormatter;

import java.util.concurrent.atomic.AtomicLong;


public class CycleNumberStringGenerator implements FastForwardableGenerator<String>,ThreadsafeGenerator {

    private AtomicLong seq = new AtomicLong(0l);
    private DateTimeFormatter formatter;

    public CycleNumberStringGenerator() {
    }

    @Override
    public String get() {
        long outval = seq.incrementAndGet();
        return String.valueOf(outval);
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}
