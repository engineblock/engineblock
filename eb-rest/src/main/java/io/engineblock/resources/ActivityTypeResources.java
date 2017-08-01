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
import io.engineblock.activityapi.ActivityType;
import io.engineblock.core.MarkdownDocInfo;
import io.engineblock.core.ActivityTypeFinder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Api(value = "/activitytypes", description = "Information about available activity types.")
@Path("/activitytypes")
public class ActivityTypeResources {


    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XHTML_XML,MediaType.APPLICATION_XML,MediaType.APPLICATION_ATOM_XML})
    @Timed
    @ApiOperation(value = "List the available activity types by canonical name.")
    public List<String> getAvailableActivityTypes() {
        return ActivityTypeFinder.instance().getAll().stream()
                .map(ActivityType::getName)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @GET
    @Path("/{activityType}/docs")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    @Timed
    @ApiOperation("Get the markdown-rendered docs for the named activity type.")
    public Response getDocsForActivityType(
            @PathParam("activityType")
            @ApiParam("The name of the activity type")
                    String activityTypeName) {

        Optional<ActivityType> activityType = ActivityTypeFinder.instance().get(activityTypeName);
        if (activityType.isPresent()) {
            Parser parser = Parser.builder().build();
            Optional<String> activityMarkdown = MarkdownDocInfo.forHelpTopic(activityTypeName);
            Node doc = parser.parse(activityMarkdown.orElseThrow(
                    () -> new RuntimeException("Unable to find help for " + activityTypeName))
            );
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String render = renderer.render(doc);
            return Response.ok(render,MediaType.TEXT_HTML_TYPE).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }
}
