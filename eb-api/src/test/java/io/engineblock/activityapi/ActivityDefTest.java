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

package io.engineblock.activityapi;

import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ActivityDefTest {

    @Test
    public void testAndDemoDefaultPositionalValues() {
        ActivityDef activityDef;

        activityDef = ActivityDef.parseActivityDef("analias");
        assertThat(activityDef.getAlias()).isEqualTo("analias");
        assertThat(activityDef.getActivityType()).isEqualTo("unknown-type");
        assertThat(activityDef.getInterCycleDelay()).isEqualTo(0);
        assertThat(activityDef.getStartCycle()).isEqualTo(0L);
        assertThat(activityDef.getEndCycle()).isEqualTo(1L);
        assertThat(activityDef.getThreads()).isEqualTo(1);
    }

    @Test
    public void testAndDemoPositionalAlias() {
        ActivityDef activityDef;

        activityDef = ActivityDef.parseActivityDef("thealias");
        assertThat(activityDef.getAlias()).isEqualTo("thealias");
    }

    @Test
    public void testAndDemoPositionalAliasType() {


        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype");
        assertThat(activityDef.getAlias()).isEqualTo("thealias");
        assertThat(activityDef.getActivityType()).isEqualTo("thetype");

    }

    @Test
    public void testAndDemoPositionalAliasTypeCycles() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;2");
        assertThat(activityDef.getAlias()).isEqualTo("thealias");
        assertThat(activityDef.getActivityType()).isEqualTo("thetype");
        assertThat(activityDef.getStartCycle()).isEqualTo(0L);
        assertThat(activityDef.getEndCycle()).isEqualTo(2L);
    }

    @Test
    public void testAndDemoPositionalAliasTypeCyclesRange() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;2..5");
        assertThat(activityDef.getAlias()).isEqualTo("thealias");
        assertThat(activityDef.getActivityType()).isEqualTo("thetype");
        assertThat(activityDef.getStartCycle()).isEqualTo(2L);
        assertThat(activityDef.getEndCycle()).isEqualTo(5L);

    }

    @Test
    public void testAndDemoPositionalAliasCyclesRangeThreads() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;2..5;100");
        assertThat(activityDef.getAlias()).isEqualTo("thealias");
        assertThat(activityDef.getActivityType()).isEqualTo("thetype");
        assertThat(activityDef.getStartCycle()).isEqualTo(2L);
        assertThat(activityDef.getEndCycle()).isEqualTo(5L);
        assertThat(activityDef.getThreads()).isEqualTo(100);
    }

    @Test
    public void testAndDemoPositionalAliasTypeCyclesRangeThreadsDelay() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;2..5;100;1000");
        assertThat(activityDef.getAlias()).isEqualTo("thealias");
        assertThat(activityDef.getActivityType()).isEqualTo("thetype");
        assertThat(activityDef.getStartCycle()).isEqualTo(2L);
        assertThat(activityDef.getEndCycle()).isEqualTo(5L);
        assertThat(activityDef.getThreads()).isEqualTo(100);

    }


    @Test
    public void testPositionalAliasAndParam() {
        ActivityDef activityDef;

        activityDef = ActivityDef.parseActivityDef("thealias;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(0L);
        assertThat(activityDef.getThreads()).isEqualTo(1);
    }

    @Test
    public void testPositionalAliasTypeAndParam() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(0L);
        assertThat(activityDef.getThreads()).isEqualTo(1);

    }

    @Test
    public void testPositionalAliasTypeCycleAndParam() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;2;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(0L);
        assertThat(activityDef.getThreads()).isEqualTo(1);

    }

    @Test
    public void testPositionAliasTypeCycleRangeAndParam() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;2..5;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(2L);
        assertThat(activityDef.getThreads()).isEqualTo(1);

    }

    @Test
    public void testPositionalAliasTypeCycleRangeThreadsAndParam() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;2..5;100;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(2L);
        assertThat(activityDef.getThreads()).isEqualTo(100);

    }

    @Test
    public void testPositionalAliasTypeCycleRangeThreadsDelayAndAParam() {

        ActivityDef activityDef = ActivityDef.parseActivityDef("thealias;thetype;2..5;100;1000;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(2L);
        assertThat(activityDef.getThreads()).isEqualTo(100);

    }

    @Test
    public void testMissingSemicolonErrorSanity() {
        ActivityDef activityDef;
        activityDef = ActivityDef.parseActivityDef("thealias;param1=val1");
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
    }

}