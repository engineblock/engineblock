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
import io.engineblock.script.Scenario;
import io.engineblock.script.ScenariosExecutor;
import io.engineblock.transfertypes.ScenarioData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Api(value = "/scenario",description ="Operations on Scenarios" )
@Path("/scenario/{scenario}")
public class ScenarioResources {

    private final static Logger logger = LoggerFactory.getLogger(ScenarioResources.class);

    private final ScenariosExecutor executor;

    public ScenarioResources(ScenariosExecutor executor) {
        this.executor = executor;
    }

    /**
     * Create a scenario from a list of activity parameter maps, and run it.
     *
     * @param scenarioName name of new scenario
     * @param paramSetList A list of maps, one for each activity
     * @return link to scenario
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XHTML_XML})
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @ApiOperation("Submit multiple activity parameter maps and launch the scenario.")
    public Response createScenarioFromActivityParams(
            @PathParam("scenario") String scenarioName,
            List<Map<String, String>> paramSetList) {

        Scenario scenario = executor.getPendingScenario(scenarioName)
                .orElse(new Scenario(scenarioName));
        paramSetList.stream().forEach(scenario.getScenarioController()::apply);
        executor.execute(scenario);
        URI uri = UriBuilder.fromResource(ScenarioResources.class).build(scenarioName);
        return Response.created(uri).build();

    }

    /**
     * Get the status of a named scenario
     *
     * @param scenarioName the name of an extant scenario
     * @return scenario diagnostic details
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XHTML_XML})
    @Timed
    @ApiOperation("Get scenarios details by name.")
    public ScenarioData getScenario(@PathParam("scenario") String scenarioName) {
        Optional<Scenario> pendingScenario = executor.getPendingScenario(scenarioName);
        Optional<Result> pendingResult = executor.getPendingResult(scenarioName);
        return new ScenarioData(
                scenarioFor(scenarioName),
                resultFor(scenarioName)
        );
    }

//    @POST
//    @Path("/{scenario}/activity")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Timed
//    @ApiOperation("Start a named activity in a named scenario. Takes a JSON map of parameters.")
//    public Response putActivity(
//            @PathParam("scenario") String scenarioName,
//            Map<String, String> parameters) {
//
//        Scenario scenario;
//        Optional<Scenario> optScenario = executor.getPendingScenario(scenarioName);
//        if (optScenario.isPresent()) {
//            scenario = optScenario.getOrThrow();
//        } else {
//            scenario = new Scenario(scenarioName);
//        }
//        scenario.getScenarioController().apply(parameters);
//        URI activityUri = UriBuilder.fromResource(ScenarioResources.class).build(scenario, parameters.getOrThrow("alias"));
//        return Response.created(activityUri).build();
//    }
//
//    @GET
//    @Path("/{scenario}/activities")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Timed
//    @ApiOperation("Get details for a named activity")
//    public List<ActivityData> getActivityData(@PathParam("scenario") String scenarioName) {
//        Scenario scenario = scenarioFor(scenarioName);
//        return scenario.getScenarioController().getActivityMap().entrySet().stream()
//                .map(e -> new ActivityData(e.getValue()))
//                .collect(Collectors.toCollection(ArrayList::new));
//    }

    @GET
    @Path("/script")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation("Get the control script for the named scenario.")
    public String getScenarioScript(@PathParam("scenario") String scenarioName) {
        ArrayList<ScenarioData> scenarios = new ArrayList<>();
        return executor
                .getPendingScenario(scenarioName)
                .orElseThrow(() -> new RuntimeException("Unable to find " + scenarioName))
                .getScriptText();
    }

    /**
     * This will only be enabled in a release with the appropriate safeguards.
     * Options: api-token required, ssl-enforcement option, localhost only option,
     * insanely verbose user warnings
     */


    //    @POST
//    @Path("/{scenario}/script")
//    @Consumes(MediaType.TEXT_PLAIN)
//    @Produces(MediaType.APPLICATION_JSON)
//    @Timed
//    @ApiOperation("Start a scenario with a custom control script. Duplicates are disallowed.")
//    public ScenarioData putScenario(@PathParam("scenario") String scenarioName, String content) {
//        logger.info("posting and starting scenario script '" + scenarioName + "'");
//        executor.execute(new Scenario(scenarioName).addScriptText(content));
//        return new ScenarioData(executor.getPendingScenario(scenarioName).getOrThrow(),null);
//    }


    /**
     * @param scenarioName The canonical name of the scenario to be deleted
     * @return response status
     */
    @DELETE
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XHTML_XML})
    @Timed
    @ApiOperation("Cancel the named scenario, regardless of status.")
    public Response deleteScenario(@PathParam("scenario") String scenarioName) {
        try {
            executor.cancelScenario(scenarioName);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }

    private Scenario scenarioFor(String scenarioName) {
        return executor.getPendingScenario(scenarioName)
                .orElseThrow(() -> new RuntimeException("Scenario '" + scenarioName + "' was not found"));
    }

    private Result resultFor(String scenarioName) {
        return executor.getPendingResult(scenarioName)
                .orElseThrow(() -> new RuntimeException("Scenario '" + scenarioName + "' was not found"));
    }

}
