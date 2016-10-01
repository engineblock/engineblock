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

package io.engineblock.app;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.engineblock.resources.ActivityTypeResources;
import io.engineblock.resources.ScenarioResources;
import io.engineblock.resources.ScenariosResources;
import io.engineblock.script.ScenariosExecutor;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class EngineBlockService extends Application<EngineBlockServiceConfig> {

    private ScenariosExecutor executor;

    public static void main(String[] args) throws Exception {
        if (args.length==0) {
            args = new String[]{ "server", "eb-rest.yaml" };
        }
        new EngineBlockService().run(args);
    }

    @Override
    public void initialize(Bootstrap<EngineBlockServiceConfig> bootstrap) {

        executor = new ScenariosExecutor("scenarios-executor for " + this.getName());

        SwaggerBundle<EngineBlockServiceConfig> swaggerBundle;
        swaggerBundle = new SwaggerBundle<EngineBlockServiceConfig>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(EngineBlockServiceConfig engineBlockServiceConfig) {
                return engineBlockServiceConfig.swaggerBundleConfiguration;
            }
        };

        bootstrap.addBundle(swaggerBundle);
        bootstrap.addBundle(new AssetsBundle("/assets/","/"));

    }

    @Override
    public void run(EngineBlockServiceConfig engineBlockServiceConfig, Environment environment) throws Exception {

        environment.jersey().register(new ScenarioResources(executor));
        environment.jersey().register(new ScenariosResources(executor));
        environment.jersey().register(new ActivityTypeResources());
    }

}
