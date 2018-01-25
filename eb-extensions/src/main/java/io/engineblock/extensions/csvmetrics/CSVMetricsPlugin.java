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
import org.slf4j.Logger;

import javax.script.ScriptContext;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CSVMetricsPlugin {
    private final ScriptContext context;
    private final Logger logger;
    private final MetricRegistry metricRegistry;

    public CSVMetricsPlugin(Logger logger, MetricRegistry metricRegistry, ScriptContext scriptContext) {
        this.logger = logger;
        this.metricRegistry = metricRegistry;
        this.context = scriptContext;
    }

    public CSVMetrics log(String filename) {
        CSVMetrics csvMetrics = new CSVMetrics(filename, logger, metricRegistry);
        writeStdout("started new csvlogger: " + filename + "\n");
        return csvMetrics;
    }

    public CSVMetrics log(String filename, long period, String timeUnit) {
        TimeUnit mappedTimeUnit = TimeUnit.valueOf(timeUnit);
        CSVMetrics csvMetrics = new CSVMetrics(filename, logger, metricRegistry, period, mappedTimeUnit);
        writeStdout("started new csvlogger: " + filename + "\n");
        return csvMetrics;
    }

    private void writeStdout(String msg) {
        try {
            context.getWriter().write(msg);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
