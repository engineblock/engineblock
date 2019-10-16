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

package io.engineblock.extensions.optimo;

import com.codahale.metrics.MetricRegistry;
import io.engineblock.extensions.ScriptingPluginInfo;
import io.virtdata.annotations.Service;
import org.slf4j.Logger;

import javax.script.ScriptContext;

@Service(ScriptingPluginInfo.class)
public class OptimoPluginData implements ScriptingPluginInfo<OptimoPlugin> {

    @Override
    public String getDescription() {
        return "Allows use of the BOBYQA optimizer in scripts.";
    }

    @Override
    public OptimoPlugin getExtensionObject(Logger logger, MetricRegistry metricRegistry, ScriptContext scriptContext) {
        return new OptimoPlugin(logger, metricRegistry, scriptContext);
    }

    @Override
    public String getBaseVariableName() {
        return "optimos";
    }
}
