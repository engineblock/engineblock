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

package io.engineblock.extensions.example;

import com.codahale.metrics.MetricRegistry;
import io.engineblock.extensions.SandboxExtensionDescriptor;
import org.slf4j.Logger;

import javax.script.ScriptContext;

@com.google.auto.service.AutoService(SandboxExtensionDescriptor.class)
public class ExampleSandboxExtensionDescriptor implements SandboxExtensionDescriptor<ExampleSandboxExtension> {

    @Override
    public String getDescription() {
        return "This is an example of a dynamically loadable script extension. It just adds two ints when" +
                "you call the getSum(...) method.";
    }

    @Override
    public ExampleSandboxExtension getExtensionObject(Logger logger, MetricRegistry metricRegistry, ScriptContext scriptContext) {
        logger.info("creating a new ExampleSandboxExtension");
        return new ExampleSandboxExtension();
    }

    @Override
    public String getExtensionName() {
        return "summer";
    }

}
