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

import io.engineblock.core.ScenariosResults;
import org.testng.annotations.Test;

@Test
public class ScenariosExecutorTest {

    @Test(enabled=false)
    public void testAwaitOnTime() {
        ScenariosExecutor e = new ScenariosExecutor(ScenariosExecutorTest.class.getSimpleName(), 1);
        Scenario s = new Scenario("testing");
        s.addScriptText("load('classpath:scripts/asyncs.js');\nsetTimeout(\"print('waited')\",5000);\n");
        e.execute(s);
        ScenariosResults scenariosResults = e.awaitAllResults();
    }



}