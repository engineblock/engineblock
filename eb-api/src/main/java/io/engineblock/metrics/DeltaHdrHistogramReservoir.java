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

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;

/**
 * A custom wrapping of snapshotting logic on the HdrHistogram. This histogram will always report the last histogram
 * since it was most recently asked for with the getDeltaSnapshot(...) method.
 * This provides local snapshot timing, but with a consistent view for reporting channels about what those snapshots
 * most recently looked like.
 */
public final class DeltaHdrHistogramReservoir implements Reservoir, HistoLogger {
    private final static Logger logger = LoggerFactory.getLogger(DeltaHdrHistogramReservoir.class);

    private final Recorder recorder;

    private Histogram runningTotals;
    private Histogram lastHistogram;

    private Histogram intervalHistogram;
    private long intervalHistogramStartTime = System.currentTimeMillis();
    private long intervalHistogramEndTime = System.currentTimeMillis();
    private ArrayList<HistogramLogWriter> attachedLoggers;
    private String metricName;

    /**
     * Create a reservoir with a default recorder. This recorder should be suitable for most usage.
     * @param name the name to give to the reservoir, for logging purposes
     */
    public DeltaHdrHistogramReservoir(String name) {
        this(name, new Recorder(4));
    }

    /**
     * Create a reservoir with a user-specified recorder.
     * @param name the name to give to the reservoir for logging purposes
     * @param recorder Recorder to use
     */
    public DeltaHdrHistogramReservoir(String name, Recorder recorder) {
        this.metricName = name;
        this.recorder = recorder;

        /*
         * Start by flipping the recorder's interval histogram.
         * - it starts our counting at zero. Arguably this might be a bad thing if you wanted to feed in
         *   a recorder that already had some measurements? But that seems crazy.
         * - intervalHistogram can be nonnull.
         * - it lets us figure out the number of significant digits to use in runningTotals.
         */
        intervalHistogram = recorder.getIntervalHistogram();
        runningTotals = new Histogram(intervalHistogram.getNumberOfSignificantValueDigits());
        lastHistogram = new Histogram(intervalHistogram.getNumberOfSignificantValueDigits());
    }

    @Override
    public int size() {
        // This appears to be infrequently called, so not keeping a separate counter just for this.
        return getSnapshot().size();
    }

    @Override
    public void update(long value) {
        recorder.recordValue(value);
    }

    /**
     * @return the data accumulated since the reservoir was created, or since the last call to this method
     */
    @Override
    public Snapshot getSnapshot() {
        Histogram delta = getDataSinceLastSnapshotAndUpdate();
        lastHistogram = delta;
        DeltaHistogramSnapshot snapshot = new DeltaHistogramSnapshot(delta);

        if (attachedLoggers != null) {
            delta.setTag(metricName);
            for (HistogramLogWriter attachedLogger : attachedLoggers) {
                attachedLogger.outputIntervalHistogram(delta);
            }
            logger.trace(
                    "wrote log histogram data for " + this.metricName + ": [" +
                            new Date(intervalHistogramStartTime) + " - " +
                            new Date(intervalHistogramEndTime) +
                            " to " + attachedLoggers.size() + " loggers"
            );

        }
        return snapshot;
    }

    /**
     * @return last histogram snapshot that was provided by {@link #getSnapshot()}
     */
    public Snapshot getLastSnapshot() {
        return new DeltaHistogramSnapshot(lastHistogram);
    }

    /**
     * Get a histogram snapshot that spans the whole lifetime of this reservoir.
     *
     * @return a histogram snapshot
     */
    public Snapshot getTotalSnapshot() {
        return new DeltaHistogramSnapshot(runningTotals);
    }

    /**
     * @return a copy of the accumulated state since the reservoir last had a snapshot
     */
    private synchronized Histogram getDataSinceLastSnapshotAndUpdate() {
        intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
        intervalHistogramStartTime = intervalHistogramEndTime;
        intervalHistogramEndTime = System.currentTimeMillis();

        lastHistogram = intervalHistogram.copy();
        runningTotals.add(lastHistogram);
        return lastHistogram;
    }

    @Override
    public synchronized void attachLogWriter(HistogramLogWriter logger) {
        if (attachedLoggers == null) {
            attachedLoggers = new ArrayList<>();
        }
        attachedLoggers.add(logger);
    }

    @Override
    public synchronized void detachLogWriter(HistogramLogWriter logger) {
        attachedLoggers.remove(logger);
        if (attachedLoggers.size() == 0) {
            attachedLoggers = null;
        }
    }
}
