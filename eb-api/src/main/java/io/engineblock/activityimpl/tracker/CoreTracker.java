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
 * allow for flexible buffering methods.
 */
public class CoreTracker implements Tracker {
    private final static Logger logger = LoggerFactory.getLogger(CoreTracker.class);
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

    public CoreTracker(long min, long max, int extentSize, int maxExtents) {
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
            logger.debug("added tracker extent [" + extent.getMin() + "," + extent.getMax() +"]");
        }
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
                            this.onFullyServed(extent);
                            ByteTrackerExtent nextExtent = extent.getNextExtent().get();
                            if (nextExtent!=null && !nextExtent.isFullyFilled()) {
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

    private void onFullyServed(ByteTrackerExtent firstReadable) {
        logger.debug("fully served: " + firstReadable);
    }

    public long getCycleInterval(int stride) {
        // TODO: Rebuild this method separate from getSegment for efficiency
        return getSegment(stride).cycle;
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
    public boolean markResult(long completedCycle, int result) {
        while (true) {
            ByteTrackerExtent extent = this.markingExtents.get();
            long remaining = extent.markResult(completedCycle, result);
            if (remaining == 0) {
                try {
                    this.mutex.acquire();
                    if (extent == this.markingExtents.get()) {
                        this.onFullyFilled(extent);
                        if (servingExtents.compareAndSet(null, extent)) {
                            logger.debug("now serving extent:" + extent);
                            lock.lock();
                            nowServing.signalAll();
                            lock.unlock();
                        }
                        markingExtents.get().extend();
                        if (!markingExtents.compareAndSet(extent,extent.getNextExtent().get())) {
                            throw new RuntimeException("Unable to advance marking extent to next extent.");
                        }
                        lock.lock();
                        nowMarking.signalAll();
                        lock.unlock();
                    } // else lost the race, just parking, waiting for more extents to write to
                    this.mutex.release();
                } catch (InterruptedException ignored) {
                }
                return true;
            } else if (remaining > 0) {
                return true;
            }
        }
    }

    private void onFullyFilled(ByteTrackerExtent extent) {
        logger.debug("fully filled: " + extent);
    }

    @Override
    public AtomicLong getMinCycle() {
        return this.min;
    }

    @Override
    public AtomicLong getMaxCycle() {
        return this.max;
    }

    @Override
    public long getPendingCycle() {
        return 3;
    }

}
