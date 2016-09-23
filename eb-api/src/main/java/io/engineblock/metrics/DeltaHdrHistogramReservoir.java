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
import org.HdrHistogram.Recorder;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A custom wrapping of snapshotting logic on the HdrHistogram. This histogram will always report the last histogram
 * since it was most recently asked for with the getDeltaSnapshot(...) method.
 * This provides local snapshot timing, but with a consistent view for reporting channels about what those snapshots
 * most recently looked like.
 */
@ThreadSafe
public final class DeltaHdrHistogramReservoir implements Reservoir {

    private final Recorder recorder;

    @GuardedBy("this")
    private Histogram runningTotals;
    private Histogram lastHistogram;

    @GuardedBy("this")
    @Nonnull
    private Histogram intervalHistogram;

    /**
     * Create a reservoir with a default recorder. This recorder should be suitable for most usage.
     */
    public DeltaHdrHistogramReservoir() {
        this(new Recorder(2));
    }

    /**
     * Create a reservoir with a user-specified recorder.
     *
     * @param recorder Recorder to use
     */
    public DeltaHdrHistogramReservoir(Recorder recorder) {
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
     * @return the data accumulated since the reservoir was created
     */
    @Override
    public Snapshot getSnapshot() {
        return new DeltaHistogramSnapshot(lastHistogram);
    }

    public Snapshot getDeltaSnapshot() {
        return new DeltaHistogramSnapshot(getDataSinceLastSnapshotAndUpdate());
    }

    public Snapshot getTotalSnapshot() {
        return new DeltaHistogramSnapshot(runningTotals);
    }

    /**
     * @return a copy of the accumulated state since the reservoir last had a snapshot
     */
    @Nonnull
    private synchronized Histogram getDataSinceLastSnapshotAndUpdate() {
        intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
        lastHistogram = intervalHistogram.copy();
        runningTotals.add(lastHistogram);
        return lastHistogram;
    }

}
