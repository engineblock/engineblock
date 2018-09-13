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

package io.engineblock.activityimpl.input;

import io.engineblock.activityapi.cyclelog.buffers.results.CycleArray;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleSegment;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class CycleArrayBufferTest {

    @Test
    public void testBasicBuffering() {
        CycleArrayBuffer b = new CycleArrayBuffer(3);
        assertThat(b.remaining()).isEqualTo(3);
        b.append(4L);
        assertThat(b.remaining()).isEqualTo(2);
        b.append(7L);
        assertThat(b.remaining()).isEqualTo(1);
        b.append(2L);
        assertThat(b.remaining()).isEqualTo(0);

        CycleArray a = b.getCycleArray();
        CycleSegment is = a.getInputSegment(3);
        long[] longs = is.nextCycles(3);
        assertThat(longs).containsExactly(4L,7L,2L);
        assertThat(a.getInputSegment(1)).isNull();

    }

}