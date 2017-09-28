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

import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class CycleResultRLETargetBufferTest {

    @Test
    public void testBasicRLEncoding() {
        CycleResultRLETargetBuffer tb = new CycleResultRLETargetBuffer();

        assertThat(tb.getBufferCapacity()).isEqualTo(1048560);

        tb.onCycleResult(0L,0);
        tb.onCycleResult(1L,1);
        CycleResultRLEBuffer r = tb.toReadable();

        ArrayList<CycleResult> cycles = new ArrayList<>();
        r.iterator().forEachRemaining(cycles::add);

        long[] cycleValues = cycles.stream().mapToLong(CycleResult::getCycle).toArray();
        assertThat(cycleValues).containsExactly(0L,1L);

        int[] resultValues = cycles.stream().mapToInt(CycleResult::getResult).toArray();
        assertThat(resultValues).containsExactly(0,1);
    }

    public void testGappedIntervalRLEEncoding() {
        CycleResultRLETargetBuffer tb = new CycleResultRLETargetBuffer(100000);

        assertThat(tb.getBufferCapacity()).isEqualTo(99994);

        tb.onCycleResult(0L,0);
        tb.onCycleResult(13L,1);
        tb.onCycleResult(14L,1);
        tb.onCycleResult(15L,1);
        tb.onCycleResult(28L,2);
        tb.onCycleResult(29L,2);
        tb.onCycleResult(100L,5);
        tb.onCycleResult(101L,6);
        tb.onCycleResult(102L,7);

        CycleResultRLEBuffer r = tb.toReadable();

        ArrayList<CycleResult> cycles = new ArrayList<>();
        r.iterator().forEachRemaining(cycles::add);

        long[] cycleValues = cycles.stream().mapToLong(CycleResult::getCycle).toArray();
        assertThat(cycleValues).containsExactly(0L,13L,14L,15L,28L,29L,100L,101L,102L);

        int[] resultValues = cycles.stream().mapToInt(CycleResult::getResult).toArray();
        assertThat(resultValues).containsExactly(0,1,1,1,2,2,5,6,7);

    }

}