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

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class CycleResultRLEBufferTest {

    @Test
    public void testRLESingle3() {
        ByteBuffer bb = ByteBuffer.allocate(3*(Long.BYTES + Long.BYTES + Byte.BYTES));
        bb.putLong(31L).putLong(32L).put((byte)127);
        bb.putLong(41L).putLong(43L).put((byte)53);
        bb.putLong(132L).putLong(135L).put((byte)27);
        bb.flip();
        CycleResultRLEBuffer crb = new CycleResultRLEBuffer(bb);

        ArrayList<CycleResult> cycles = new ArrayList<>();
        crb.iterator().forEachRemaining(cycles::add);
        long[] cycleValues = cycles.stream().mapToLong(CycleResult::getCycle).toArray();
        int[] resultValues= cycles.stream().mapToInt(CycleResult::getResult).toArray();
        assertThat(cycleValues).containsExactly(31L,41L,42L,132L,133L,134L);
        assertThat(resultValues).containsExactly(127,53,53,27,27,27);
    }

}