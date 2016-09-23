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
import org.assertj.core.data.Offset;
import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class ScriptTests {

    public static Result runScenario(String scriptname) {
        String scenarioName = "testing activity" + scriptname;
        ScenariosExecutor e = new ScenariosExecutor(ScriptTests.class.getSimpleName() + ":" + scriptname, 1);
        Scenario s = new Scenario(scenarioName);
        s.addScriptText("load('classpath:scripts/" + scriptname + ".js');");
        e.execute(s);
        ScenariosResults scenariosResults = e.awaitAllResults();
        Result result = scenariosResults.getOne();
        result.reportToLog();
        return result;
    }

    @Test
    public void testRateLimiter() {
        Result result = runScenario("ratelimiter");
        String iolog = result.getIOLog();
        System.out.println("iolog\n"+iolog);
        Pattern p = Pattern.compile(".*mean rate = (\\d[.\\d]+).*",Pattern.DOTALL);
        Matcher m = p.matcher(iolog);
        assertThat(m.matches()).isTrue();

        String digits = m.group(1);
        assertThat(digits).isNotEmpty();
        double rate = Double.valueOf(digits);
        assertThat(rate).isCloseTo(100.0, Offset.offset(10.0));
    }

    @Test
    public void testExtensionPoint() {
        Result result = runScenario("extensions");
        assertThat(result.getIOLog()).contains("sum is 46");
    }

    @Test
    public void testExtensionCsvLogger() {
        Result result = runScenario("extension_csvmetrics");
        assertThat(result.getIOLog()).contains("started new csvlogger: csvmetricstestdir");
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
        result.reportToLog();
        int startedAt = result.getIOLog().indexOf("starting activity teststartstopdiag");
        int stoppedAt = result.getIOLog().indexOf("stopped activity teststartstopdiag");
        assertThat(startedAt).isGreaterThan(0);
        assertThat(stoppedAt).isGreaterThan(startedAt);
    }

    @Test
    public void testThreadChange() {
        Result result = runScenario("threadchange");
        int changedTo1At = result.getIOLog().indexOf("threads now 1");
        int changedTo5At = result.getIOLog().indexOf("threads now 5");
        assertThat(changedTo1At).isGreaterThan(0);
        assertThat(changedTo5At).isGreaterThan(changedTo1At);
    }

    @Test
    public void testReadMetric() {
        Result result = runScenario("readmetrics");
        assertThat(result.getIOLog()).contains("count: ");
    }


}