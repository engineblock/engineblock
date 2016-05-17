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
import io.engineblock.script.Scenario;
import io.engineblock.script.ScenariosExecutor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Api("/scenarios")
@Path("/scenarios")
public class ScenariosResources {

    private final static Logger logger = LoggerFactory.getLogger(ScenariosResources.class);

    private final ScenariosExecutor executor;

    public ScenariosResources(ScenariosExecutor executor) {
        this.executor = executor;
    }

    @GET
    @Path("/")
    @ApiOperation("List all known scenarios.")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public List<String> getScenarioList() {
        return executor.getPendingScenarios();
    }


}
