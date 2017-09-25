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

package io.engineblock.activityapi.cycletracking;

import io.engineblock.activityapi.cycletracking.buffers.CycleResult;
import io.engineblock.activityapi.cycletracking.buffers.CycleResultRawBuffer;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class CycleResultRawBufferTest {

    @Test
    public void testBBCycleRecorder() {
        ByteBuffer bb = ByteBuffer.allocate(5 * (Long.BYTES + Byte.BYTES));

        bb.putLong(33L).put((byte)0);
        bb.putLong(34L).put((byte)1);
        bb.putLong(35L).put((byte)3);
        bb.putLong(36L).put((byte)1);
        bb.putLong(39L).put((byte)0);
        bb.flip();

        CycleResultRawBuffer cycleResults = new CycleResultRawBuffer(bb);
        int index=-1;
        for (CycleResult cycleResult : cycleResults) {
            index++;
            switch (index) {
                case 0:
                    assertThat(cycleResult.getCycle()).isEqualTo(33L);
                    assertThat(cycleResult.getResult()).isEqualTo((byte)0);
                    break;
                case 1:
                    assertThat(cycleResult.getCycle()).isEqualTo(34L);
                    assertThat(cycleResult.getResult()).isEqualTo((byte)1);
                    break;
                case 2:
                    assertThat(cycleResult.getCycle()).isEqualTo(35L);
                    assertThat(cycleResult.getResult()).isEqualTo((byte)3);
                    break;
                case 3:
                    assertThat(cycleResult.getCycle()).isEqualTo(36L);
                    assertThat(cycleResult.getResult()).isEqualTo((byte)1);
                    break;
                case 4:
                    assertThat(cycleResult.getCycle()).isEqualTo(39L);
                    assertThat(cycleResult.getResult()).isEqualTo((byte)0);
                    break;
                default:
                     throw new RuntimeException("out of range on test");
            }

        }

    }

}