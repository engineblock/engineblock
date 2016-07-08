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

package io.engineblock.core;

import io.engineblock.script.Scenario;
import io.engineblock.script.ScenariosExecutor;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScenariosResults {

    private String scenariosExecutorName;
    private Map<Scenario,Result> scenarioResultMap = new LinkedHashMap<>();
    private Result one;


    public ScenariosResults(ScenariosExecutor scenariosExecutor) {
        this.scenariosExecutorName =scenariosExecutor.getName();
    }

    public ScenariosResults(ScenariosExecutor scenariosExecutor, Map<Scenario, Result> map) {
        this.scenariosExecutorName = scenariosExecutor.getName();
        scenarioResultMap.putAll(map);
    }

    public void reportSummaryTo(PrintStream out) {
        for (Map.Entry<Scenario, Result> entry : this.scenarioResultMap.entrySet()) {
            Scenario scenario = entry.getKey();
            Result oresult = entry.getValue();

            out.println("results for scenario: " + scenario);

            if (oresult!=null) {
                oresult.reportTo(out);
            } else {
                out.println(": incomplete (missing result)");
            }
        }
    }

    public Result getOne() {
        if (this.scenarioResultMap.size()!=1) {
            throw new RuntimeException("getOne found " + this.scenarioResultMap.size() + " results instead of 1.");
        }
        return scenarioResultMap.values().stream().findFirst().orElseThrow(
                () -> new RuntimeException("Missing result."));
    }
}
