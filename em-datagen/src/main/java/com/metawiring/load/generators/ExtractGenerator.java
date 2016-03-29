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

public class ExtractGenerator implements Generator<String> {

    private final static Logger logger = LoggerFactory.getLogger(ExtractGenerator.class);
    private static CharBuffer fileDataImage =null;

    private int minsize, maxsize;
    MersenneTwister rng = new MersenneTwister(System.nanoTime());
    private IntegerDistribution sizeDistribution;
    private IntegerDistribution positionDistribution;
    private String fileName;

    public ExtractGenerator(String fileName, int minsize, int maxsize) {
        this.fileName = fileName;
        this.minsize = minsize;
        this.maxsize = maxsize;
    }

    public ExtractGenerator(String fileName, String minsize, String maxsize) {
        this(fileName, Integer.valueOf(minsize), Integer.valueOf(maxsize));
    }

    @Override
    public String get() {

        if (fileDataImage == null) {
            synchronized (ExtractGenerator.class) {
                if (fileDataImage == null) {
                    CharBuffer image= loadFileData();
                    fileDataImage = image;

                }
            }
        }

        if (sizeDistribution==null)
        {
            sizeDistribution = new UniformIntegerDistribution(rng, minsize, maxsize);
            positionDistribution = new UniformIntegerDistribution(rng, 1, fileDataImage.limit() - maxsize);
        }

        int offset = positionDistribution.sample();
        int length = sizeDistribution.sample();
        String sub = null;
        try {
            sub = fileDataImage.subSequence(offset, offset + length).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sub;
    }

    private CharBuffer loadFileData() {
        InputStream stream = null;
        File onFileSystem= new File("data" + File.separator + fileName);

        if (onFileSystem.exists()) {
            try {
                stream = new FileInputStream(onFileSystem);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Unable to find file " + onFileSystem.getPath() + " after verifying that it exists.");
            }
            logger.debug("Loaded file data from " + onFileSystem.getPath());
        }

        if (stream==null) {
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("data/"+fileName);
            logger.debug("Loaded file data from classpath resource " + fileName);
        }

        if (stream == null) {
            throw new RuntimeException(fileName + " was missing.");
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
