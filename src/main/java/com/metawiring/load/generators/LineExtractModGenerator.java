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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class LineExtractModGenerator implements FastForwardableGenerator<String> {
    private final static Logger logger = LoggerFactory.getLogger(LineExtractModGenerator.class);

    private AtomicLong atomicLong = new AtomicLong();
    private List<String> lines = new ArrayList<>();

    private String filename;

    public LineExtractModGenerator(String filename) {
        this.filename = filename;
        loadLines(this.filename);
    }

    @Override
    public String get() {
        int itemIdx = (int) (atomicLong.incrementAndGet() % lines.size());
        String item = lines.get(itemIdx);
        return item;
    }

    public String toString() {
        return getClass().getSimpleName() + ":" + filename;
    }

    private void loadLines(String filename) {

        InputStream stream = LineExtractModGenerator.class.getClassLoader().getResourceAsStream(filename);
        if (stream == null) {
            throw new RuntimeException(filename + " was missing.");
        }

        CharBuffer linesImage;
        try {
            InputStreamReader isr = new InputStreamReader(stream);
            linesImage = CharBuffer.allocate(1024 * 1024);
            isr.read(linesImage);
            isr.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        linesImage.flip();
        Collections.addAll(lines, linesImage.toString().split("\n"));
    }

    @Override
    public void fastForward(long fastForwardTo) {
        atomicLong.set(fastForwardTo);
    }
}
