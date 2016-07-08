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

package io.engineblock.script;

import io.engineblock.core.Result;
import io.engineblock.core.ScenariosResults;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class ScriptTests {

    @Test
    public void testBlockingRun() {
        String scenarioName = "testing activity start and stop";
        ScenariosExecutor e = new ScenariosExecutor(ScriptTests.class.getSimpleName() + ":testBlockingRun", 1);
        Scenario s = new Scenario(scenarioName);
        s.addScriptText("load('classpath:scripts/blockingrun.js');");
        e.execute(s);
        ScenariosResults scenariosResults = e.awaitAllResults();
        Result result  =scenariosResults.getOne();
        result.reportTo(System.out);

        int a1end = result.getIOLog().indexOf("blockingactivity1 finished");
        int a2start = result.getIOLog().indexOf("running blockingactivity2");
        assertThat(a1end).isLessThan(a2start);

    }

    @Test
    public void testStartStop() {
        ScenariosExecutor e = new ScenariosExecutor(ScriptTests.class.getSimpleName() + ":testStartStop");
        Scenario s = new Scenario("testing activity start and stop");
        s.addScriptText("load('classpath:scripts/startstopdiag.js');");
        e.execute(s);
        ScenariosResults scenariosResults = e.awaitAllResults();
        Result result  =scenariosResults.getOne();
        result.reportTo(System.out);
    }

    @Test
    public void testThreadChange() {
        ScenariosExecutor e = new ScenariosExecutor(ScriptTests.class.getSimpleName() + ":testThreadChange");
        Scenario s = new Scenario("testing thread changes");
        s.addScriptText("load('classpath:scripts/threadchange.js');");
        e.execute(s);
        ScenariosResults scenariosResults = e.awaitAllResults();
        Result result  =scenariosResults.getOne();
        result.reportTo(System.out);
    }

    @Test
    public void testReadMetric() {
        ScenariosExecutor e = new ScenariosExecutor(ScriptTests.class.getSimpleName() + ":testReadMetric");
        Scenario s = new Scenario("testing metric sandbox variables for read");
        s.addScriptText("load('classpath:scripts/readmetrics.js');");
        e.execute(s);
        ScenariosResults scenariosResults = e.awaitAllResults();
        Result result  =scenariosResults.getOne();
        result.reportTo(System.out);
    }


}