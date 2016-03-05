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
import de.greenrobot.common.hash.Murmur3F;

public class Murmur3Div implements FastForwardableGenerator<Long> {
    private Murmur3F murmur3f = new Murmur3F();
    private LongDivSequenceGenerator longDivSequenceGenerator;

    public Murmur3Div(long divisor) {
        this.longDivSequenceGenerator = new LongDivSequenceGenerator(divisor);
    }
    public Murmur3Div(String divisor) {
        this(Long.valueOf(divisor));
    }

    @Override
    public void fastForward(long fastForwardTo) {
        murmur3f = new Murmur3F((int) (fastForwardTo % Integer.MAX_VALUE));
    }

    @Override
    public Long get() {
        long divided= longDivSequenceGenerator.get();
        murmur3f.update((int) (divided % Integer.MAX_VALUE));
        return murmur3f.getValue();
    }
}
