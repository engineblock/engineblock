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

import io.engineblock.core.ScenarioLogger;
import io.engineblock.core.ScenarioResult;
import io.engineblock.core.ScenariosResults;
import org.assertj.core.data.Offset;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class ScriptTests {

    public static ScenarioResult runScenario(String scriptname, String... params) {
        if ((params.length % 2)!=0) {
            throw new RuntimeException("params must be pairwise key, value, ...");
        }
        Map<String,String> paramsMap = new HashMap<>();

        for (int i = 0; i < params.length; i+=2) {
            paramsMap.put(params[i],params[i+1]);
        }
        String scenarioName = "testing activity" + scriptname;
        ScenariosExecutor e = new ScenariosExecutor(ScriptTests.class.getSimpleName() + ":" + scriptname, 1);
        Scenario s = new Scenario(scenarioName);
        s.addScenarioScriptParams(paramsMap);
        s.addScriptText("load('classpath:scripts/" + scriptname + ".js');");
        ScenarioLogger scenarioLogger = new ScenarioLogger(s).setMaxLogs(0).setLogDir("logs/test").start();
        e.execute(s,scenarioLogger);
        ScenariosResults scenariosResults = e.awaitAllResults();
        ScenarioResult scenarioResult = scenariosResults.getOne();
        scenarioResult.reportToLog();
        return scenarioResult;
    }

    @Test
    public void testTargetRatePhased() {

        ScenarioResult scenarioResult = runScenario("target_rate");
        String iolog = scenarioResult.getIOLog();
        System.out.println("iolog\n"+iolog);
        Pattern p = Pattern.compile(".*mean phase rate = (\\d[.\\d]+).*",Pattern.DOTALL);
        Matcher m = p.matcher(iolog);
        assertThat(m.matches()).isTrue();

        String digits = m.group(1);
        assertThat(digits).isNotEmpty();
        double rate = Double.valueOf(digits);
        assertThat(rate).isCloseTo(15000, Offset.offset(1000.0));
    }

    @Test
    public void testStrideRateOnly() {
        ScenarioResult scenarioResult = runScenario("stride_rate");
        String iolog = scenarioResult.getIOLog();
        System.out.println("iolog\n"+iolog);
        Pattern p = Pattern.compile(".*stride_rate.strides.meanRate = (\\d[.\\d]+).*", Pattern.DOTALL);
        Matcher m = p.matcher(iolog);
        assertThat(m.matches()).isTrue();

        String digits = m.group(1);
        assertThat(digits).isNotEmpty();
        double rate = Double.valueOf(digits);
        assertThat(rate).isCloseTo(25000.0D,Offset.offset(5000D));
    }

    @Test
    public void testPhaseRateOnly() {
        ScenarioResult scenarioResult = runScenario("phase_rate");
        String iolog = scenarioResult.getIOLog();
        System.out.println("iolog\n"+iolog);
        Pattern p = Pattern.compile(".*phase_rate.phases.meanRate = (\\d[.\\d]+).*", Pattern.DOTALL);
        Matcher m = p.matcher(iolog);
        assertThat(m.matches()).isTrue();

        String digits = m.group(1);
        assertThat(digits).isNotEmpty();
        double rate = Double.valueOf(digits);
        assertThat(rate).isCloseTo(25000.0D,Offset.offset(5000D));
    }


    @Test
    public void testExtensionPoint() {
        ScenarioResult scenarioResult = runScenario("extensions");
        assertThat(scenarioResult.getIOLog()).contains("sum is 46");
    }

    @Test
    public void testLinkedInput() {
        ScenarioResult scenarioResult = runScenario("linkedinput");
        Pattern p = Pattern.compile(".*started leader.*started follower.*stopped leader.*stopped follower.*",
                Pattern.DOTALL);
        assertThat(p.matcher(scenarioResult.getIOLog()).matches()).isTrue();
    }

    @Test
    public void testExtensionCsvLogger() {
        ScenarioResult scenarioResult = runScenario("extension_csvmetrics");
        assertThat(scenarioResult.getIOLog()).contains("started new csvlogger: csvmetricstestdir");
    }


    @Test
    public void testScriptParamsVariable() {
        ScenarioResult scenarioResult = runScenario("params_variable","one", "two", "three", "four");
        assertThat(scenarioResult.getIOLog()).contains("params.get(\"one\")='two'");
        assertThat(scenarioResult.getIOLog()).contains("params.get(\"three\")='four'");
        assertThat(scenarioResult.getIOLog()).contains("params.size()=2");
        assertThat(scenarioResult.getIOLog()).contains("params.get(\"three\") [overridden-three-five]='five'");
        assertThat(scenarioResult.getIOLog()).contains("params.get(\"four\") [defaulted-four-niner]='niner'");
    }

    @Test
    public void testExtensionHistoStatsLogger() throws IOException {
        ScenarioResult scenarioResult = runScenario("extension_histostatslogger");
        assertThat(scenarioResult.getIOLog()).contains("stdout started logging to histostats.csv");
        List<String> strings = Files.readAllLines(Paths.get("histostats.csv"));
        String logdata = strings.stream().collect(Collectors.joining("\n"));
        assertThat(logdata).contains("min,p25,p50,p75,p90,p95,");
        assertThat(logdata.split("Tag=testhistostatslogger.cycles,").length).isGreaterThanOrEqualTo(3);
    }

    @Test
    public void testExtensionHistogramLogger() throws IOException {
        ScenarioResult scenarioResult = runScenario("extension_histologger");
        assertThat(scenarioResult.getIOLog()).contains("stdout started logging to hdrhistodata.log");
        List<String> strings = Files.readAllLines(Paths.get("hdrhistodata.log"));
        String logdata = strings.stream().collect(Collectors.joining("\n"));
        assertThat(logdata).contains(",HIST");
        assertThat(logdata.split("Tag=testhistologger.cycles,").length).isGreaterThanOrEqualTo(3);
    }

    @Test
    public void testBlockingRun() {
        ScenarioResult scenarioResult = runScenario("blockingrun");
        int a1end = scenarioResult.getIOLog().indexOf("blockingactivity1 finished");
        int a2start = scenarioResult.getIOLog().indexOf("running blockingactivity2");
        assertThat(a1end).isLessThan(a2start);
    }

    @Test
    public void testAwaitFinished() {
        ScenarioResult scenarioResult = runScenario("awaitfinished");
        scenarioResult.reportToLog();
    }

    @Test
    public void testStartStop() {
        ScenarioResult scenarioResult = runScenario("startstopdiag");
        scenarioResult.reportToLog();
        int startedAt = scenarioResult.getIOLog().indexOf("starting activity teststartstopdiag");
        int stoppedAt = scenarioResult.getIOLog().indexOf("stopped activity teststartstopdiag");
        assertThat(startedAt).isGreaterThan(0);
        assertThat(stoppedAt).isGreaterThan(startedAt);
    }

    @Test
    public void testThreadChange() {
        ScenarioResult scenarioResult = runScenario("threadchange");
        int changedTo1At = scenarioResult.getIOLog().indexOf("threads now 1");
        int changedTo5At = scenarioResult.getIOLog().indexOf("threads now 5");
        assertThat(changedTo1At).isGreaterThan(0);
        assertThat(changedTo5At).isGreaterThan(changedTo1At);
    }

    @Test
    public void testReadMetric() {
        ScenarioResult scenarioResult = runScenario("readmetrics");
        assertThat(scenarioResult.getIOLog()).contains("count: ");
    }

    @Test
    public void testExceptionPropagationFromMotorThread() {
        ScenarioResult scenarioResult = runScenario("activityerror");
        assertThat(scenarioResult.getException()).isPresent();
        assertThat(scenarioResult.getException().get().getMessage()).contains("For input string: \"unparsable\"");
    }

    @Test
    public void testExceptionPropagationFromActivityInit() {
        ScenarioResult scenarioResult = runScenario("activityiniterror");
        assertThat(scenarioResult.getException()).isPresent();
        assertThat(scenarioResult.getException().get().getMessage()).contains("For input string: \"unparsable\"");
        assertThat(scenarioResult.getException()).isNotNull();
    }



}