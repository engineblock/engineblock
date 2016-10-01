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

import com.codahale.metrics.Histogram;


public class NicerHistogram extends Histogram implements DeltaSnapshotter {

    private final DeltaHdrHistogramReservoir hdrDeltaReservoir;
    private long cacheExpiryMillis = 0L;
    private long cacheTimeMillis = 0L;

    public NicerHistogram(DeltaHdrHistogramReservoir hdrHistogramReservoir) {
        super(hdrHistogramReservoir);
        this.hdrDeltaReservoir = hdrHistogramReservoir;
    }

    @Override
    public DeltaSnapshotReader getDeltaReader() {
        return new DeltaSnapshotReader(this);
    }


    /**
     * Only return a new snapshot form current reservoir data if the cached one has expired.
     * @return a new delta snapshot, or the cached one
     */
    @Override
    public ConvenientSnapshot getSnapshot() {
        if (System.currentTimeMillis()<cacheExpiryMillis) {
            return new ConvenientSnapshot(hdrDeltaReservoir.getLastSnapshot());
        } else {
            return new ConvenientSnapshot(hdrDeltaReservoir.getSnapshot());
        }
    }

    public ConvenientSnapshot getDeltaSnapshot(long cacheTimeMillis) {
        this.cacheTimeMillis= cacheTimeMillis;
        cacheExpiryMillis = System.currentTimeMillis() + cacheTimeMillis;
        return new ConvenientSnapshot(hdrDeltaReservoir.getSnapshot());
    }

    public ConvenientSnapshot getTotalSnapshot() {
        return new ConvenientSnapshot(hdrDeltaReservoir.getTotalSnapshot());
    }
}
