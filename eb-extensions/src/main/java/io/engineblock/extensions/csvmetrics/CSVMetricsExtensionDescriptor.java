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

package io.engineblock.extensions.csvmetrics;

import com.codahale.metrics.MetricRegistry;
import com.google.auto.service.AutoService;
import io.engineblock.extensions.SandboxExtensionDescriptor;
import org.slf4j.Logger;

import javax.script.ScriptContext;

@AutoService(SandboxExtensionDescriptor.class)
public class CSVMetricsExtensionDescriptor implements SandboxExtensionDescriptor<CSVMetricsExtension> {
    @Override
    public String getDescription() {
        return "Allows a script to log some or all metrics to CSV files";
    }

    @Override
    public CSVMetricsExtension getExtensionObject(Logger logger, MetricRegistry metricRegistry, ScriptContext scriptContext) {
        return new CSVMetricsExtension(logger, metricRegistry, scriptContext);
    }

    @Override
    public String getExtensionName() {
        return "csvmetrics";
    }

}
