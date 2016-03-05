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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.CharBuffer;

public class LoremExtractGenerator implements Generator<String> {

    private final static Logger logger = LoggerFactory.getLogger(LoremExtractGenerator.class);

    private static CharBuffer loremIpsumImage=null;

    private int minsize, maxsize;
    MersenneTwister rng = new MersenneTwister(System.nanoTime());
    private IntegerDistribution sizeDistribution;
    private IntegerDistribution positionDistribution;

    public LoremExtractGenerator(int minsize, int maxsize) {
        this.minsize = minsize;
        this.maxsize = maxsize;
    }

    public LoremExtractGenerator(String minsize, String maxsize) {
        this(Integer.valueOf(minsize), Integer.valueOf(maxsize));
    }

    @Override
    public String get() {

        if (loremIpsumImage == null) {
            synchronized (LoremExtractGenerator.class) {
                if (loremIpsumImage == null) {
                    CharBuffer image= loadLoremIpsum();
                    loremIpsumImage = image;
                }
                sizeDistribution = new UniformIntegerDistribution(rng, minsize, maxsize);
                positionDistribution = new UniformIntegerDistribution(rng, 1, loremIpsumImage.limit() - maxsize);
            }
        }

        int offset = positionDistribution.sample();
        int length = sizeDistribution.sample();
        String sub = null;
        try {
            sub = loremIpsumImage.subSequence(offset, offset + length).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sub;
    }

    private CharBuffer loadLoremIpsum() {
        InputStream stream = LoremExtractGenerator.class.getClassLoader().getResourceAsStream("data/lorem_ipsum_full.txt");
        if (stream == null) {
            throw new RuntimeException("lorem_ipsum_full.txt was missing.");
        }

        CharBuffer image;
        try {
            InputStreamReader isr = new InputStreamReader(stream);
            image = CharBuffer.allocate(1024 * 1024);
            isr.read(image);
            isr.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        image.flip();


        return image.asReadOnlyBuffer();

    }

    public String toString() {
        return getClass().getSimpleName() + ":" + minsize + ":" + maxsize;
    }
}
