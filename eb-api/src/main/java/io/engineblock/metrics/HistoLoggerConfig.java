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

package io.engineblock.metrics;

import com.codahale.metrics.Metric;
import org.HdrHistogram.HistogramLogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HistoLoggerConfig extends HistoLoggerListener {
    private final static Logger logger = LoggerFactory.getLogger(HistoLoggerConfig.class);

    private final String sessionName;
    private File logfile;
    private HistogramLogWriter writer;
    private Pattern pattern;
    private Map<String, Metric> activeMetrics = new HashMap<>();

    public HistoLoggerConfig(String sessionName, File file, Pattern pattern) {
        this.sessionName = sessionName;
        this.logfile = file;
        this.pattern = pattern;
        start();
    }

    public boolean matches(String metricName) {
        return pattern.matcher(metricName).matches();
    }

    /**
     * By convention, it is typical for the logging application
     * to use a comment to indicate the logging application at the head
     * of the log, followed by the log format version, a start time,
     * and a legend (in that order).
     */
    public void start() {
        try {
            writer = new HistogramLogWriter(logfile);
            writer.outputComment("logging histograms for session " + sessionName);
            writer.outputLogFormatVersion();
            long currentTimeMillis = System.currentTimeMillis();
            writer.outputStartTime(currentTimeMillis);
            writer.setBaseTime(currentTimeMillis);
            writer.outputLegend();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error while starting histogram log writer", e);
        }
    }

    @Override
    public void onHistogramLogCapableAdded(String name, HistoLogger capable) {
        if (matches(name)) {
            logger.debug("attaching logger to " + name);
            capable.attachLogWriter(this.getLogWriter());
        }
    }

    @Override
    public void onHistogramLogCapableRemoved(String name, HistoLogger capable) {
        if (matches(name)) {
            logger.debug("unattaching logger to " + name);
            capable.detachLogWriter(this.getLogWriter());
        }
    }

    public HistogramLogWriter getLogWriter() {
        return writer;
    }
}
