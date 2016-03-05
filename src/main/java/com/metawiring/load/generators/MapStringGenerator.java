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
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.HashMap;
import java.util.Map;

public class MapStringGenerator implements Generator<String> {

    private LineExtractGenerator paramGenerator;
    private IntegerDistribution sizeDistribution;
    private MersenneTwister rng = new MersenneTwister(System.nanoTime());

    public MapStringGenerator(String paramFile, int sizeDistribution) {
        this.sizeDistribution = new UniformIntegerDistribution(0,sizeDistribution-1);
        this.paramGenerator = new LineExtractGenerator(paramFile);
    }

    public MapStringGenerator(String paramFile, String sizeDistribution) {
        this(paramFile, Integer.valueOf(sizeDistribution));
    }

    @Override
    public String get() {
        int mapSize = sizeDistribution.sample();
        StringBuilder sb = new StringBuilder(100);
        for (int idx=0;idx<mapSize;idx++) {
            sb.append(paramGenerator.get()).append(":");
            sb.append(paramGenerator.get()).append(";");
        }
        return sb.toString();
    }
}
