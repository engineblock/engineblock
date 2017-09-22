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

package io.engineblock.util;

import org.assertj.core.data.Offset;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class UnitParserTests {

    @Test
    public void testCountParser() {
        assertThat(Unit.countFor("1M")).isPresent().contains(1000000.0d);
        assertThat(Unit.convertCounts(Unit.Count.KILO,"1M")).isPresent().contains(1000.0d);
        assertThat(Unit.convertCounts(Unit.Count.MEGA, "1K")).isPresent().contains(0.001d);
    }

    @Test
    public void testDurationParser() {
        assertThat(Unit.msFor("1000")).contains(1000L);
        assertThat(Unit.msFor("1S")).contains(1000L);
        assertThat(Unit.msFor("1 SECOND")).contains(1000L);
        assertThat(Unit.msFor("5d")).contains((long)86400*1000*5);
        assertThat(Unit.durationFor(Unit.Duration.HOUR,"5 days")).contains(120L);
    }

    @Test
    public void testBytesParser() {
        assertThat(Unit.convertBytes(Unit.Bytes.KIB,"1 byte").get()).isCloseTo((1.0/1024.0), Offset.offset(0.000001D));
        assertThat(Unit.convertBytes(Unit.Bytes.GB,"1 megabyte").get()).isCloseTo((1/1000.0),Offset.offset(0.000001D));
        assertThat(Unit.convertBytes(Unit.Bytes.GB,"1 GiB").get())
                .isCloseTo(
                        ((1024.0D*1024.0D*1024.0D)/(1000.0D*1000.0D*1000.0D)),
                        Offset.offset(0.0000001D));
        assertThat(Unit.bytesFor("1.43 MB").get()).isCloseTo(1430000.0D,Offset.offset(0.00001D));
        assertThat(Unit.bytesFor("1KiB").get()).isCloseTo(1024.0D,Offset.offset(0.00001D));
    }

}