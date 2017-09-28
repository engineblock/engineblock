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

package io.engineblock.activityimpl.marker;

import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.cycletracking.markers.SegmentMarker;
import io.engineblock.activityapi.cycletracking.markers.Marker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is the default cycle marker implementation for EngineBlock.
 * <p>
 * This cycle marker wraps another tracking structure in order to
 * allow for flexible buffering methods. The extents are buffer segments
 * which can be managed atomically. They are chained here in two chains:
 * The marking chain and the tracking chain. When the atomic marking head
 * is non-null, then marking is possible, but marking calls block otherwise.
 * The same is true for the tracking head element.
 * <p>
 * The nowMarking and nowTracking conditions are meant to be locked and awaited
 * by marking and tracking calls respectively. Conversely, they are expected
 * to be signaled by tracking and marking calls.
 * <p>
 * This implementation needs to be adapted to handle early exit of either
 * marker or tracker threads with no deadlock.
 */
public class CoreMarker implements Marker {

    private final static Logger logger = LoggerFactory.getLogger(CoreMarker.class);
    private final int extentSize;
    private final int maxExtents;
    private List<SegmentMarker> readers = new ArrayList<>();
    private AtomicLong min;
    private AtomicLong nextMin;
    private AtomicReference<ByteTrackerExtent> markingExtents = new AtomicReference<>();
    private ReentrantLock lock = new ReentrantLock(false);
    private Condition nowMarking = lock.newCondition();
    private Semaphore mutex = new Semaphore(1, false);

    public CoreMarker(long min, long nextRangeMin, int extentSize, int maxExtents) {
        this.min = new AtomicLong(min);
        this.nextMin = new AtomicLong(nextRangeMin);
        this.extentSize = extentSize;
        this.maxExtents = maxExtents;
        initExtents();
    }

    public CoreMarker(Activity activity) {
        this.min = new AtomicLong(activity.getActivityDef().getStartCycle());
        this.nextMin = new AtomicLong(activity.getActivityDef().getEndCycle());
        long stride = activity.getParams().getOptionalLong("stride").orElse(1L);
        long cycleCount = nextMin.get()-min.get();
        if ((cycleCount%stride)!=0) {
            throw new RuntimeException("stride must evenly divide into cycles.");
            // TODO: Consider setting cycles to " ...
        }
        this.extentSize = calculateExtentSize(cycleCount, stride);
        this.maxExtents = 3;
        initExtents();
    }

    private void initExtents() {
        ByteTrackerExtent extent = new ByteTrackerExtent(min.get(), (min.get() + extentSize));
        this.markingExtents.set(extent);
        for (int i = 0; i < maxExtents; i++) {
            extent = extent.extend();
            logger.debug("added tracker extent " + extent.rangeSummary());
        }
        logger.info("using max " + maxExtents + " extents with size: " + extentSize);
    }


    @Override
    public boolean onCycleResult(long completedCycle, int result) {
        while (true) {
            ByteTrackerExtent extent = this.markingExtents.get();
            long unmarked = extent.markResult(completedCycle, result);

            if (unmarked > 0) {
                return true;
            } else if (unmarked == 0) {
                try {
                    mutex.acquire();
                    ByteTrackerExtent head = this.markingExtents.get();
                    while (head.isFullyFilled()) {
                        head.extend();
                        if (!this.markingExtents.compareAndSet(head, head.getNextExtent().get())) {
                            throw new RuntimeException("Unable to swap head extent.");
                        }
                        onFullyFilled(head);
                        head=this.markingExtents.get();
                    }
                    mutex.release();
                } catch (InterruptedException ignored) {
                }
                return true;
            }
        }
    }

    @Override
    public void close() throws Exception {
        mutex.acquire();
        ByteTrackerExtent e = this.markingExtents.get();
        while (e != null) {
            onFullyFilled(e);
            e = e.getNextExtent().get();
        }
        mutex.release();
    }

    private void onFullyFilled(ByteTrackerExtent extent) {
        logger.debug("MARKER>: fully filled: " + extent);
        for (SegmentMarker reader : readers) {
            reader.onCycleSegment(extent.getSegment());
        }
    }

    private void onFullyServed(ByteTrackerExtent firstReadable) {
        logger.debug("TRACKER: fully tracked: " + firstReadable);
    }

    public void addExtentReader(SegmentMarker reader) {
        this.readers.add(reader);
    }

    public void removeExtentReader(SegmentMarker reader) {
        this.readers.remove(reader);
    }

    private int calculateExtentSize(long cycleCount, long stride) {
        if (cycleCount<=2000000) {
            return (int) cycleCount;
        }
        for (int cs=2000000;  cs>500000;  cs--) {
            if ((cycleCount%cs)==0 && (cs%stride)==0) {
                return cs;
            }
        }
        throw new RuntimeException("no even divisor of cycleCount and Stride between 500K and 2M, with cycles=" + cycleCount +",  and stride=" + stride);
    }


}
