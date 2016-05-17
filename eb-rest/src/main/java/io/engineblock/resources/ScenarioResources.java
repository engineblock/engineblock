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
import io.engineblock.core.Result;
import io.engineblock.transfertypes.ScenarioData;
import io.engineblock.script.Scenario;
import io.engineblock.script.ScenariosExecutor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Optional;

@Api("/scenario")
@Path("/scenario")
public class ScenarioResources {

    private final static Logger logger = LoggerFactory.getLogger(ScenarioResources.class);

    private final ScenariosExecutor executor;

    public ScenarioResources(ScenariosExecutor executor) {
        this.executor = executor;
    }

    /**
     * Get the status of a named scenario
     *
     * @param scenarioName the name of an extant scenario
     * @return scenario diagnostic details
     */
    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @ApiOperation("Get scenarios details by name.")
    public ScenarioData getScenario(@PathParam("name") String scenarioName) {
        Optional<Scenario> pendingScenario = executor.getPendingScenario(scenarioName);
        Optional<Result> pendingResult = executor.getPendingResult(scenarioName);
        return new ScenarioData(
                pendingScenario.orElseThrow(() -> new RuntimeException("Unable to find scenario " + scenarioName)),
                pendingResult.orElse(new Result("result is pending."))
        );
    }

    @GET
    @Path("{name}/script")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation("Get the control script for the named scenario.")
    public String getScenarioScript(@PathParam("name") String scenarioName) {
        ArrayList<ScenarioData> scenarios = new ArrayList<>();
        return executor
                .getPendingScenario(scenarioName)
                .orElseThrow(() -> new RuntimeException("Unable to find " + scenarioName))
                .getScriptText();
    }

    @POST
    @Path("/{name}/script")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @ApiOperation("Start a scenario with a custom control script. Duplicates are disallowed.")
    public ScenarioData putScenario(@PathParam("name") String scenarioName, String content) {
        logger.info("posting and starting scenario script '" + scenarioName + "'");
        executor.execute(new Scenario(scenarioName).addScriptText(content));
        return new ScenarioData(executor.getPendingScenario(scenarioName).get(),null);
    }

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @ApiOperation("Cancel the named scenario, regardless of status.")
    public Response deleteScenario(@PathParam("name") String scenarioName) {
        try {
            executor.cancelScenario(scenarioName);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }
}
