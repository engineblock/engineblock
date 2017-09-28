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

package io.engineblock.activityimpl.marker;

import io.engineblock.activityapi.cycletracking.buffers.CycleSegment;
import io.engineblock.activityapi.cycletracking.markers.SegmentMarker;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Test
public class CoreMarkerTest {

    @Test
    public void testCoreSimple0to4() {
        CoreMarker ct4 = new CoreMarker(0,3,4,1);
        TestReader r = new TestReader();
        ct4.addExtentReader(r);
        ct4.onCycleResult(0,0);
        ct4.onCycleResult(1,1);
        ct4.onCycleResult(2,2);
        ct4.onCycleResult(3,3);
        Assertions.assertThat(r.segments).hasSize(1);
        Assertions.assertThat(r.segments.get(0).cycle).isEqualTo(0L);
        Assertions.assertThat(r.segments.get(0).codes.length).isEqualTo(4);
    }

    private static class TestReader implements SegmentMarker {
        List<CycleSegment> segments = new ArrayList<>();

        @Override
        public void onCycleSegment(CycleSegment segment) {
            segments.add(segment);
        }

        @Override
        public void close() throws Exception {

        }
    }
}