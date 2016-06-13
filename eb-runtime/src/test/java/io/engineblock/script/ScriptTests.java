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
import org.testng.annotations.Test;

import java.util.Map;

@Test
public class ScriptTests {

    @Test
    public void testThreadChange() {
        ScenariosExecutor e = new ScenariosExecutor(1);
        Scenario s = new Scenario("testing thread changes");
        s.addScriptText("load('classpath:scripts/threadchange.js');");
        e.execute(s);
        Map<Scenario, Result> stringResultMap = e.awaitAllResults();
        e.reportSummaryTo(System.out);
    }

    @Test
    public void testReadMetric() {
        ScenariosExecutor e = new ScenariosExecutor(1);
        Scenario s = new Scenario("testing metric sandbox variables for read");
        s.addScriptText("load('classpath:scripts/readmetrics.js');");
        e.execute(s);

        Map<Scenario, Result> stringResultMap = e.awaitAllResults();
        e.reportSummaryTo(System.out);

    }


}