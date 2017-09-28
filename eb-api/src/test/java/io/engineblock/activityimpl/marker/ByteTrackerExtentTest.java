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
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class ByteTrackerExtentTest {

    @Test
    public void testOrdered4() {
        ByteTrackerExtent bt4 = new ByteTrackerExtent(33, 37);
        bt4.markResult(36L,3);
        bt4.markResult(33L,0);
        bt4.markResult(35L,2);
        assertThat(bt4.getMarkerData()[3]).isEqualTo((byte)3);
        assertThat(bt4.getMarkerData()[0]).isEqualTo((byte)0);
        assertThat(bt4.getMarkerData()[2]).isEqualTo((byte)2);
        assertThat(bt4.isFullyFilled()).isFalse();
        bt4.markResult(34L,1);
        assertThat(bt4.getMarkerData()[1]).isEqualTo((byte)1);
        assertThat(bt4.isFullyFilled()).isTrue();

        CycleSegment seg1 = bt4.getSegment(2);
        assertThat(seg1.cycle).isEqualTo(33);
        assertThat(seg1.codes.length).isEqualTo(2);
        assertThat(seg1.codes[0]).isEqualTo((byte)0);
        assertThat(bt4.isFullyServed()).isFalse();
        CycleSegment seg2 = bt4.getSegment(2);
        assertThat(bt4.isFullyServed()).isTrue();
    }


    @Test
    public void testFullCounter() {
        ByteTrackerExtent bt4 = new ByteTrackerExtent(33, 37);
        assertThat(bt4.markResult(36L,3)).isEqualTo(3);
        assertThat(bt4.markResult(33L,0)).isEqualTo(2);
        assertThat(bt4.markResult(35L,2)).isEqualTo(1);
        assertThat(bt4.markResult(35L,2)).isEqualTo(0);

        assertThat(bt4.markResult(33L,9)).isEqualTo(-1);
    }


}