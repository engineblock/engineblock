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

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class CSVLogger {

    CsvReporter reporter;

    /**
     * Create a CSV reporter that is not automatically logging.
     * @param directory a CSV logging filename
     * @param logger an extension logger, to be used for logging extension-specific events
     * @param registry a MetricRegistry to report
     */
    public CSVLogger(String directory, Logger logger, MetricRegistry registry) {
        File reportTo = new File(directory);
        if (!reportTo.exists()) {
            if (!reportTo.mkdirs()) {
                throw new RuntimeException("Unable to make directory: " + reportTo);
            }
        }

        reporter = CsvReporter.forRegistry(registry)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build(reportTo);

    }

    /**
     * Create an autologging CSV Reporter with the specified period and time unit.
     * @param csvFile a CSV logging filename
     * @param logger an extension logger, to be used for logging extension-specific events
     * @param registry a MetricRegistry to report
     * @param period a period between reporting intervals
     * @param timeUnit the actual timeunit for the period
     */
    public CSVLogger(String csvFile, Logger logger, MetricRegistry registry, long period, TimeUnit timeUnit) {
        this(csvFile, logger, registry);
        reporter.start(period, timeUnit);
    }

    public void start(long period, String timeUnitName) {
        TimeUnit timeUnit = TimeUnit.valueOf(timeUnitName);
        reporter.start(period, timeUnit);
    }

    public void stop() {
        reporter.stop();
    }

    public void report() {
        reporter.report();
    }
}
