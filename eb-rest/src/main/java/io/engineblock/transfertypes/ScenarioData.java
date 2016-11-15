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

package io.engineblock.transfertypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.core.Result;
import io.engineblock.script.Scenario;

import java.util.ArrayList;
import java.util.List;

public class ScenarioData {

    private final String name;
    private final List<ActivityDef> activityDefs;
    private final List<String> iolog;
    private Result result;
    private String logBuffer;

    public ScenarioData(Scenario scenario, Result result) {
        this.name = scenario.getName();
        this.activityDefs = scenario.getScenarioController().getActivityDefs();
        if (scenario.getIOLog().isPresent()) {
            this.iolog = scenario.getIOLog().get();
//            this.iolog = scenario.getIOLog().getOrThrow().stream().collect(Collectors.joining());
        } else {
            this.iolog = new ArrayList<String>();
//            this.iolog = "not logged data";
        }
//        Optional<BufferAppender> logBuffer = scenario.getLogBuffer();
//        if (logBuffer.isPresent()) {
//            this.logBuffer = logBuffer.get().toString();
//        }
        this.result = result;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public List<ActivityDef> getActivityDefs() {
        return activityDefs;
    }

    @JsonProperty
    public List<String> getIolog() {
        return iolog;
    }

    @JsonProperty
    public Result getResult() {
        return result;
    }

    @JsonProperty
    public String getLogBuffer() {
        return this.logBuffer;
    }
}
