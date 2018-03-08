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

import io.engineblock.activityapi.planning.ElementSequencer;
import io.engineblock.activityapi.planning.IntervalSequencer;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class IntervalSequencerTest {

    private static ElementSequencer<AnEvent> seqr = new IntervalSequencer<>();

    @Test
    public void testFixture() {
        List<AnEvent> events = fromText("A:1,B:15");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).name).isEqualTo("A");
        assertThat(events.get(0).ratio).isEqualTo(1L);
        assertThat(events.get(1).name).isEqualTo("B");
        assertThat(events.get(1).ratio).isEqualTo(15L);
    }

    @Test
    public void testA8B4C2D1() {
        List<AnEvent> events = fromText("A:8,B:4,C:2,D:1");
        String seq = seqr.sequenceSummary(events, ratioFunc, "");
        assertThat(seq).isEqualTo("ABCDAABAABCAABA");
    }

    @Test
    public void testO4I3() {
        List<AnEvent> events = fromText("O:4,I:3");
        String seq = seqr.sequenceSummary(events, ratioFunc, "");
        assertThat(seq).isEqualTo("OIOIOIO");
    }

    @Test
    public void testO4I4() {
        List<AnEvent> events = fromText("O:4,I:4");
        String seq = seqr.sequenceSummary(events, ratioFunc,"");
        assertThat(seq).isEqualTo("OIOIOIOI");
    }
    @Test
    public void testO4I5() {
        List<AnEvent> events = fromText("O:4,I:5");
        String seq = seqr.sequenceSummary(events, ratioFunc,"");
        assertThat(seq).isEqualTo("OIIOIOIOI");
    }


    private static List<AnEvent> fromText(String text) {
        String[] texts = text.split(",");
        List<AnEvent> events = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            String elemText = texts[i];
            String[] words = elemText.split(":",2);
            events.add(new AnEvent(words[0],Long.parseLong(words[1])));
        }
        return events;
    }

    private static ToLongFunction<AnEvent> ratioFunc = value -> value.ratio;

    private static class AnEvent {
        public final long ratio;
        public final String name;

        public AnEvent(String name, long ratio) {
            this.name = name;
            this.ratio = ratio;
        }

        public String toString() {
            return name;
        }

    }

}