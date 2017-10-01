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

package io.engineblock.activityimpl.tracker;

import io.engineblock.activityapi.cycletracking.buffers.results.CycleResultsIntervalSegment;
import io.engineblock.activityimpl.marker.ByteTrackerExtent;
import io.engineblock.activityimpl.marker.CoreTracker;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class CoreTrackerTest {

    @Test
    public void testBasicTracking() {
        CoreTracker ct = new CoreTracker();
        ct.onExtent(new ByteTrackerExtent(3, new int[] {13,14,15,16,17}));
        CycleResultsIntervalSegment s = ct.getCycleResultsSegment(5);
        assertThat(s.cycle).isEqualTo(3L);
        assertThat(s.codes).containsExactly((byte)13,(byte)14,(byte)15,(byte)16,(byte)17);
    }

}