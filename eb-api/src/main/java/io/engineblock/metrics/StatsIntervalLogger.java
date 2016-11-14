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

import org.HdrHistogram.EncodableHistogram;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * HistoIntervalLogger runs a separate thread to snapshotAndWrite encoded histograms on a regular interval.
 * It listens to the metrics registry for any new metrics that match the pattern. Any metrics
 * which both match the pattern and which are {@link EncodableHistogram}s are written the configured
 * logfile at the configured interval.
 */
public class StatsIntervalLogger extends CapabilityHook<AttachingHdrDeltaHistoProvider> implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(StatsIntervalLogger.class);

    private final String sessionName;
    //    private final long intervalMillis;
    private long intervalLength;
    private File logfile;
    private HistoStatsCSVWriter writer;
    private Pattern pattern;

    private Map<String, HdrDeltaHistoProvider> histoMetrics = new LinkedHashMap<>();
    private PeriodicRunnable<StatsIntervalLogger> executor;

    public StatsIntervalLogger(String sessionName, File file, Pattern pattern, long intervalLength) {
        this.sessionName = sessionName;
        this.logfile = file;
        this.pattern = pattern;
        this.intervalLength = intervalLength;
        startLogging();
    }

    public boolean matches(String metricName) {
        return pattern.matcher(metricName).matches();
    }

    /**
     * By convention, it is typical for the logging application
     * to use a comment to indicate the logging application at the head
     * of the log, followed by the log format version, a startLogging time,
     * and a legend (in that order).
     */
    public void startLogging() {
        writer = new HistoStatsCSVWriter(logfile);
        writer.outputComment("logging histograms for session " + sessionName);
        writer.outputLogFormatVersion();
        long currentTimeMillis = System.currentTimeMillis();
        writer.outputStartTime(currentTimeMillis);
        writer.setBaseTime(currentTimeMillis);
        writer.outputLegend();

        this.executor = new PeriodicRunnable<StatsIntervalLogger>(this.getInterval(), this);
        executor.startThread();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HistoLogger:" + this.pattern + ":" + this.logfile.getPath() + ":" + this.intervalLength);
        return sb.toString();
    }

    public long getInterval() {
        return intervalLength;
    }

    @Override
    public void onCapableAdded(String name, AttachingHdrDeltaHistoProvider chainedHistogram) {
        if (pattern.matcher(name).matches()) {
            this.histoMetrics.put(name, chainedHistogram.attach());
        }
    }

    @Override
    public void onCapableRemoved(String name, AttachingHdrDeltaHistoProvider capable) {
        this.histoMetrics.remove(name);
    }

    @Override
    protected Class<AttachingHdrDeltaHistoProvider> getCapabilityClass() {
        return AttachingHdrDeltaHistoProvider.class;
    }

    @Override
    public void run() {
        for (Map.Entry<String, HdrDeltaHistoProvider> histMetrics : histoMetrics.entrySet()) {
            String metricName = histMetrics.getKey();
            HdrDeltaHistoProvider hdrDeltaHistoProvider = histMetrics.getValue();
            Histogram nextHdrHistogram = hdrDeltaHistoProvider.getNextHdrDeltaHistogram();
            writer.writeInterval(nextHdrHistogram);
        }

    }
}
