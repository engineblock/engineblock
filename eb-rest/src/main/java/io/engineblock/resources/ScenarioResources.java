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

package io.engineblock.resources;

import com.codahale.metrics.annotation.Timed;
import io.engineblock.restapi.Scenario;
import io.engineblock.script.ScenariosExecutor;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

@Api("/scenario")
@Path("/scenario")
public class ScenarioResources {

    private final ScenariosExecutor executor;

    public ScenarioResources(ScenariosExecutor executor) {
        this.executor = executor;
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Scenario getScenario(@PathParam("name") String scenarioName) {
        Optional<ScenarioContext> context = executor.getScenario(scenarioName);
        return context.map(Scenario::new)

    }
}
