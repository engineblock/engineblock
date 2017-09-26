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

package io.engineblock.activityimpl.tracker;

import io.engineblock.activityapi.cycletracking.Marker;
import io.engineblock.activityapi.cycletracking.CycleSegment;
import io.engineblock.activityapi.cycletracking.Tracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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
 * The nowMarking and nowServing conditions are meant to be locked and awaited
 * by marking and serving calls respectively. Conversely, they are expected
 * to be signaled by serving and marking calls.
 * <p>
 * This implementation needs to be adapted to handle early exit of either
 * marker or tracker threads with no deadlock.
 */
public class CoreMarker implements Marker, Tracker {

    private final static Logger logger = LoggerFactory.getLogger(CoreMarker.class);

    private final int extentSize;
    private final int maxExtents;
    private AtomicLong min;
    private AtomicLong max;
    private AtomicReference<ByteTrackerExtent> markingExtents = new AtomicReference<>();
    private AtomicReference<ByteTrackerExtent> servingExtents = new AtomicReference<>();
    private ReentrantLock lock = new ReentrantLock(false);
    private Condition nowMarking = lock.newCondition();
    private Condition nowServing = lock.newCondition();
    private Semaphore mutex = new Semaphore(1, false);

    public CoreMarker(long min, long max, int extentSize, int maxExtents) {
        this.min = new AtomicLong(min);
        this.max = new AtomicLong(max);
        this.extentSize = extentSize;
        this.maxExtents = maxExtents;
        initExtents();
    }

    private void initExtents() {
        ByteTrackerExtent extent = new ByteTrackerExtent(min.get(), (min.get() + extentSize) - 1);
        this.markingExtents.set(extent);
        for (int i = 0; i < maxExtents - 1; i++) {
            extent = extent.extend();
            logger.debug("added tracker extent [" + extent.getMin() + "," + extent.getMax() + "]");
        }
        logger.info("using " + maxExtents + " max extents with size: " + extentSize);
    }

    private void onFullyServed(ByteTrackerExtent firstReadable) {
        logger.debug("fully served: " + firstReadable);
    }

    /**
     * <OL>
     * <LI>Mark the correct and active extent if possible.</LI>
     * <LI>If the marked extent was completed, attempt to activate a serving extent,
     * but only if the serving extent was not set (atomically). If successful,
     * signal now Serving.</LI>
     * <LI>If an active extent was marked, return</LI>
     * <LI>Add an extent, if the the maxExtents limit is not reached.</LI>
     * <LI>If an extent was added, retry request.</LI>
     * <LI>If an extent was not added, block for nowMarking state, when signaled, retry request.</LI>
     * </OL>
     *
     * @param completedCycle The cycle to mark isCycleCompleted
     * @param result         The result code to be marked
     * @return true if the result could be marked, otherwise false
     */
    @Override
    public boolean onCycleResult(long completedCycle, int result) {
        while (true) {
            ByteTrackerExtent extent = this.markingExtents.get();

            // Await markable extents, only done by setup and the tracker side
            if (extent == null) {
                logger.trace("marker extents unset, awaiting available extent for cycle:result" + completedCycle + ":" + result);
                while (extent == null) {
                    try {
                        lock.lock();
                        nowMarking.await();
                        lock.unlock();
                    } catch (InterruptedException ignored) {
                    }
                    extent = this.markingExtents.get();
                }
                logger.trace("marker extent set, proceeding");
            }

            // attempt to mark cycle and result on all available marking extents
            long unmarked = extent.markResult(completedCycle, result);

            if (unmarked == 0) { // There were ZERO remaining unmarked cycles in all active extents
                try {
                    this.mutex.acquire(); // this is done only when all extents are full

                    // setting the initial TRACKING extent IFF the first marking extent was extant //
                    if (extent == this.markingExtents.get()) {
                        this.onFullyFilled(extent);
                        // try to activate a serving extent, if none is set
                        if (servingExtents.compareAndSet(null, extent)) {
                            logger.debug("now serving extent:" + extent);
                            lock.lock();
                            nowServing.signalAll();
                            lock.unlock();
                        }
                    }
                    // advance the first marking extent to the next available
                    // This includes the case in which the next extent is null, which is
                    // effectively the same as disabling marking until the head
                    // marking extent is defined again.
                    markingExtents.get().extend();
                    if (!markingExtents.compareAndSet(extent, extent.getNextExtent().get())) {
                        throw new RuntimeException("Unable to advance marking extent to next extent.");
                    }
                    lock.lock();
                    nowMarking.signalAll();
                    lock.unlock();
//                        nowMarking.await(); // else lost the race, just parking, waiting for more extents to write to
                    this.mutex.release();
                } catch (InterruptedException ignored) {
                }

            } else if (unmarked > 0) {
                return true;
            }
        }

    }

    private void onFullyFilled(ByteTrackerExtent extent) {
        logger.debug("fully filled: " + extent);
    }

    @Override
    public CycleSegment getSegment(int stride) {
        CycleSegment segment = null;
        while (true) {
            ByteTrackerExtent extent = servingExtents.get();
            if (extent == null) {
                try {
                    lock.lock();
                    nowServing.await(10, TimeUnit.SECONDS);
                    lock.unlock();
                } catch (InterruptedException ignored) {
                }
                continue;
            }

            segment = extent.getSegment(stride);
            if (segment != null) {
                return segment;
            } else {
                if (extent.isFullyServed()) {
                    try {
                        mutex.acquire();
                        if (servingExtents.get() == extent) {
                            onFullyServed(extent);
                            ByteTrackerExtent nextExtent = extent.getNextExtent().get();
                            if (nextExtent != null && !nextExtent.isFullyFilled()) {
                                nextExtent = null;
                            }
                            if (!servingExtents.compareAndSet(extent, nextExtent)) {
                                throw new RuntimeException("exclusive access but CAS failed??");
                            }
                            if (servingExtents.get() != null) {
                                lock.lock();
                                nowServing.signalAll();
                                lock.unlock();
                            }
                            lock.lock();
                            nowMarking.signalAll();
                            lock.unlock();
                        } // else Lost the race, just parking, waiting for more extents to read from
                        mutex.release();
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
                throw new RuntimeException("extent was not fully served and was unable to return a segment:" + extent);
            }
        }
    }

    @Override
    public AtomicLong getMinCycle() {
        return min;
    }

    @Override
    public AtomicLong getMaxCycle() {
        return max;
    }

    @Override
    public long getPendingCycle() {
        return 0;
    }

    public long getCycleInterval(int stride) {
        // TODO: Rebuild this method separate from getSegment for efficiency
        return getSegment(stride).cycle;
    }


}
