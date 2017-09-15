/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package io.engineblock.planning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.ToLongFunction;

public class SequencePlanner<T> {
    private final static Logger logger = LoggerFactory.getLogger(SequencePlanner.class);

    private final List<T> elements;
    private int[] elementIndex;

    public enum SequencerType {
        bucket,
        interval,
        concat
    }

    public SequencePlanner(List<T> elements, ToLongFunction<T> ratioFunc, SequencerType sequencerType) {
        switch (sequencerType) {
            case bucket:
                logger.trace("sequencing elements by simple round-robin");
                this.elementIndex = new BucketSequencer<T>().sequenceByIndex(elements,ratioFunc);
                break;
            case interval:
                logger.trace("sequencing elements by interval and position");
                this.elementIndex = new IntervalSequencer<T>().sequenceByIndex(elements,ratioFunc);
                break;
            case concat:
                logger.trace("sequencing elements by concatenation");
                this.elementIndex = new ConcatSequencer<T>().sequenceByIndex(elements,ratioFunc);
        }
        this.elements = elements;
    }

    public T get(long selector) {
        int index = (int) (selector % elementIndex.length);
        index = elementIndex[index];
        return elements.get(index);
    }
}
