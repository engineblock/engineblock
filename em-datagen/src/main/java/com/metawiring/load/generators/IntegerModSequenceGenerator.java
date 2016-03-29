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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;

public class IntegerModSequenceGenerator implements FastForwardableGenerator<Integer> {
    private final static Logger logger = LoggerFactory.getLogger(IntegerModSequenceGenerator.class);

    private final int modulo;
//    AtomicInteger seq=new AtomicInteger(0);
    int seq=0;

    public IntegerModSequenceGenerator(int modulo) {
        this.modulo=modulo;
    }
    public IntegerModSequenceGenerator(String modulo) {
        this(Integer.valueOf(modulo));
    }

    @Override
    public Integer get() {
        seq++;
        int ret = seq % modulo;
        return ret;
//        int s = seq.incrementAndGet();
//        //logger.info("mod s:" + s);
//        return (s % modulo);
    }

    @Override
    public void fastForward(long fastForwardTo) {
        if (fastForwardTo>Integer.MAX_VALUE) {
            throw new InvalidParameterException("Unable to fast forward an int sequence generator with value " + fastForwardTo);
        }
        seq = (int) fastForwardTo;
//        seq = new AtomicInteger((int)fastForwardTo);
    }
}