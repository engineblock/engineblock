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
    private SequencerType sequencerType;
    private List<T> elements;
    private List<Long> ratios;
    private int[] elementIndex;

    public enum SequencerType {
        bucket,
        interval,
        concat
    }

    public SequencePlanner(SequencerType sequencerType) {
        this.sequencerType = sequencerType;
    }

    public void addOp(T elem, ToLongFunction<T> ratioFunc) {
        this.elements.add(elem);
        this.ratios.add(ratioFunc.applyAsLong(elem));
    }

    public void addOp(T elem, long func) {
        this.elements.add(elem);
        this.ratios.add(func);
    }

    public OpSequence<T> resolve() {
        switch (sequencerType) {
            case bucket:
                logger.trace("sequencing elements by simple round-robin");
                this.elementIndex = new BucketSequencer<T>().seqIndexesByRatios(elements,ratios);
                break;
            case interval:
                logger.trace("sequencing elements by interval and position");
                this.elementIndex = new IntervalSequencer<T>().seqIndexesByRatios(elements,ratios);
                break;
            case concat:
                logger.trace("sequencing elements by concatenation");
                this.elementIndex = new ConcatSequencer<T>().seqIndexesByRatios(elements,ratios);
        }
        this.elements = elements;
        return new Sequence<>(elements,elementIndex);
    }

//    public SequencePlanner(List<T> elements, List<Long> ratios, SequencerType sequencerType) {
//        switch (sequencerType) {
//            case bucket:
//                logger.trace("sequencing elements by simple round-robin");
//                this.elementIndex = new BucketSequencer<T>().seqIndexesByRatios(elements,ratios);
//                break;
//            case interval:
//                logger.trace("sequencing elements by interval and position");
//                this.elementIndex = new IntervalSequencer<T>().seqIndexesByRatios(elements,ratios);
//                break;
//            case concat:
//                logger.trace("sequencing elements by concatenation");
//                this.elementIndex = new ConcatSequencer<T>().seqIndexesByRatios(elements,ratios);
//        }
//        this.elements = elements;
//    }
//
//    public SequencePlanner(List<T> elements, ToLongFunction<T> ratioFunc, SequencerType sequencerType) {
//        switch (sequencerType) {
//            case bucket:
//                logger.trace("sequencing elements by simple round-robin");
//                this.elementIndex = new BucketSequencer<T>().seqIndexByRatioFunc(elements,ratioFunc);
//                break;
//            case interval:
//                logger.trace("sequencing elements by interval and position");
//                this.elementIndex = new IntervalSequencer<T>().seqIndexByRatioFunc(elements,ratioFunc);
//                break;
//            case concat:
//                logger.trace("sequencing elements by concatenation");
//                this.elementIndex = new ConcatSequencer<T>().seqIndexByRatioFunc(elements,ratioFunc);
//        }
//        this.elements = elements;
//    }
//
    public static class Sequence<T> implements OpSequence<T> {
        private final List<T> elems;
        private final int[] seq;

        public Sequence(List<T> elems, int[] seq) {
            this.elems = elems;
            this.seq = seq;
        }

        @Override
        public T get(long selector) {
            int index = (int) (selector % seq.length);
            index = seq[index];
            return elems.get(index);
        }

    @Override
    public int opCount() {
        return elems.size();
    }
}

}
