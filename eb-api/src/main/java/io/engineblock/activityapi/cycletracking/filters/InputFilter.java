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

package io.engineblock.activityapi.cycletracking.filters;

import io.engineblock.activityapi.cycletracking.buffers.CycleResultFilter;
import io.engineblock.activityapi.input.Input;

import java.util.function.IntPredicate;

public abstract class InputFilter implements CycleResultFilter {

    private Input input;
    private IntPredicate predicate;

    public InputFilter setInput(Input input) {
        this.input = input;
        return this;
    }

    public InputFilter setPredicate(IntPredicate predicate) {
        this.predicate = predicate;
        return this;
    }

//    @Override
//    public InputSegment getInputSegment(int segmentLength) {
//        CycleArrayBuffer buf = new CycleArrayBuffer(segmentLength);
//        while (buf.remaining()>0) {
//            int remaining = buf.remaining();
//            InputSegment inputSegment = input.getInputSegment(remaining);
//        }
//        input.getInputSegment(segmentLength);
//        input.getInputSegment(1);
//        input.getInputSegment()
//        return null;
//    }
}
