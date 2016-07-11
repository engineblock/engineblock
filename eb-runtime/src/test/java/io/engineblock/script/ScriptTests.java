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

    private Result runScenario(String scriptname) {
        String scenarioName = "testing activity" + scriptname;
        ScenariosExecutor e = new ScenariosExecutor(ScriptTests.class.getSimpleName() + ":" + scriptname, 1);
        Scenario s = new Scenario(scenarioName);
        s.addScriptText("load('classpath:scripts/" + scriptname + ".js');");
        e.execute(s);
        ScenariosResults scenariosResults = e.awaitAllResults();
        Result result = scenariosResults.getOne();
        result.reportTo(System.out);

        return result;
    }

    @Test(enabled = false)
    public void testSpeedSanity() {
        Result result = runScenario("speedcheck");
    }

    @Test(enabled = false)
    public void testThreadSpeeds() {
        Result result = runScenario("threadspeeds");
    }


    @Test
    public void testRateLimiter() {
        Result result = runScenario("ratelimiter");
    }

    @Test
    public void testBlockingRun() {
        Result result = runScenario("blockingrun");
        int a1end = result.getIOLog().indexOf("blockingactivity1 finished");
        int a2start = result.getIOLog().indexOf("running blockingactivity2");
        assertThat(a1end).isLessThan(a2start);

    }

    @Test
    public void testStartStop() {
        Result result = runScenario("startstopdiag");
        result.reportTo(System.out);
    }

    @Test
    public void testThreadChange() {
        Result result = runScenario("threadchange");
    }

    @Test
    public void testReadMetric() {
        Result result = runScenario("readmetric");
    }


}