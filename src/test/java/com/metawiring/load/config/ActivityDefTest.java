package com.metawiring.load.config;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/*
*   Copyright 2015 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http;//www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
public class ActivityDefTest {

    @Test
    public void testParser() {
        ActivityDef activityDef;

        activityDef = ActivityDef.parseActivityDef("thename");
        assertThat(activityDef.getAlias()).isEqualTo("thename");

        activityDef = ActivityDef.parseActivityDef("thename;thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getSource()).isEqualTo("thesource");

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getEndCycle()).isEqualTo(2l);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2..5");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(2l);
        assertThat(activityDef.getEndCycle()).isEqualTo(5l);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2..5;100");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(2l);
        assertThat(activityDef.getEndCycle()).isEqualTo(5l);
        assertThat(activityDef.getThreads()).isEqualTo(100);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2..5;100;1000");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(2l);
        assertThat(activityDef.getEndCycle()).isEqualTo(5l);
        assertThat(activityDef.getThreads()).isEqualTo(100);
        assertThat(activityDef.getMaxAsync()).isEqualTo(1000);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2..5;100;1000;5000");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(2l);
        assertThat(activityDef.getEndCycle()).isEqualTo(5l);
        assertThat(activityDef.getThreads()).isEqualTo(100);
        assertThat(activityDef.getMaxAsync()).isEqualTo(1000);
        assertThat(activityDef.getInterCycleDelay()).isEqualTo(5000);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getEndCycle()).isEqualTo(2l);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2;100");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getEndCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);
        assertThat(activityDef.getMaxAsync()).isEqualTo(100);
        assertThat(activityDef.getInterCycleDelay()).isEqualTo(0);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2;100;1000");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getEndCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);
        assertThat(activityDef.getMaxAsync()).isEqualTo(1000);
        assertThat(activityDef.getInterCycleDelay()).isEqualTo(0);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2;100;1000;5000");
        assertThat(activityDef.getSource()).isEqualTo("thesource");
        assertThat(activityDef.getAlias()).isEqualTo("thename");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getEndCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);
        assertThat(activityDef.getMaxAsync()).isEqualTo(1000);
        assertThat(activityDef.getInterCycleDelay()).isEqualTo(5000);

    }

    @Test
    public void testParserWithOptions() {
        ActivityDef activityDef;

        activityDef = ActivityDef.parseActivityDef("thename;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getThreads()).isEqualTo(1);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getThreads()).isEqualTo(1);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getThreads()).isEqualTo(1);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2..5;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(1);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2..5;100;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2..5;100;1000;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2..5;100;1000;5000;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getThreads()).isEqualTo(1);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2;100;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getEndCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2;100;1000;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getEndCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);

        activityDef = ActivityDef.parseActivityDef("thename;thesource;2;100;1000;5000;param1=val1;");
        assertThat(activityDef).isNotNull();
        assertThat(activityDef.getParams()).isNotNull();
        assertThat(activityDef.getParams().getStringOrDefault("param1", "invalid")).isEqualTo("val1");
        assertThat(activityDef.getStartCycle()).isEqualTo(1l);
        assertThat(activityDef.getEndCycle()).isEqualTo(2l);
        assertThat(activityDef.getThreads()).isEqualTo(100);

    }

    @Test
    public void testMissingSemicolonErrorSanity() {
        ActivityDef activityDef;
        activityDef = ActivityDef.parseActivityDef("thename;param1=val1");
        assertThat(activityDef.getParams().getStringOrDefault("param1","invalid")).isEqualTo("val1");
    }

}