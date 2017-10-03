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

package io.engineblock.activityapi.cycletracking.buffers;

import io.engineblock.activityapi.cycletracking.buffers.results.CycleResult;
import io.engineblock.activityapi.cycletracking.buffers.results.CycleResultsSegment;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public interface CycleResultSegmentsReadable extends Iterable<CycleResultsSegment> {
//    /**
//     * @param stride The number of contiguous cycles that must be provided
//     * @return a {@link CycleResultsIntervalSegment}
//     */
//    CycleResultsSegment getCycleResultsSegment(int stride);

    default Iterable<CycleResult> getCycleResultIterable() {
        return new Iterable<CycleResult>() {
            @NotNull
            @Override
            public Iterator<CycleResult> iterator() {
                return new Iterator<CycleResult>() {
                    Iterator<CycleResultsSegment> iterSegment = CycleResultSegmentsReadable.this.iterator();
                    Iterator<CycleResult> innerIter=iterSegment.next().iterator();

                    @Override
                    public boolean hasNext() {
                        while(!innerIter.hasNext()&&iterSegment.hasNext()) {
                            innerIter=iterSegment.next().iterator();
                        }
                        return innerIter.hasNext();
//
//                        if (iterSegment == null || (innerIter!=null && !innerIter.hasNext())) {
//                            iterSegment = CycleResultSegmentsReadable.this.iterator();
//                            if (iterSegment.hasNext()) {
//                                innerIter = iterSegment.next().iterator();
//                            } else {
//                                return false;
//                            }
//                        }
//                        return innerIter.hasNext();
                    }

                    @Override
                    public CycleResult next() {
                        return innerIter.next();
                    }

                };
            }
        };
    }
}
