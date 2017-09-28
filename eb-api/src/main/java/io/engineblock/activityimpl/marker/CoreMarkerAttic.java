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

import io.engineblock.activityapi.cycletracking.buffers.CycleSegment;
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
 * The nowMarking and nowTracking conditions are meant to be locked and awaited
 * by marking and tracking calls respectively. Conversely, they are expected
 * to be signaled by tracking and marking calls.
 * <p>
 * This implementation needs to be adapted to handle early exit of either
 * marker or tracker threads with no deadlock.
 */
public class CoreMarkerAttic {

    private final static Logger logger = LoggerFactory.getLogger(CoreMarkerAttic.class);

    private final int extentSize;
    private final int maxExtents;
    private AtomicLong min;
    private AtomicLong max;
    private AtomicReference<ByteTrackerExtent> markingExtents = new AtomicReference<>();
    private AtomicReference<ByteTrackerExtent> trackingExtents = new AtomicReference<>();
    private ReentrantLock lock = new ReentrantLock(false);
    private Condition nowMarking = lock.newCondition();
    private Condition nowTracking = lock.newCondition();
    private Semaphore mutex = new Semaphore(1, false);

    public CoreMarkerAttic(long min, long max, int extentSize, int maxExtents) {
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
            logger.debug("added tracker extent " + extent.rangeSummary());
        }
        logger.info("using " + maxExtents + " max extents with size: " + extentSize);
    }

    private void onFullyServed(ByteTrackerExtent firstReadable) {
        logger.debug("TRACKER: fully tracked: " + firstReadable);
    }

    /**
     * <OL>
     * <LI>Mark the correct and active extent if possible.</LI>
     * <LI>If the marked extent was completed, attempt to activate a tracking extent,
     * but only if the tracking extent was not set (atomically). If successful,
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
    public boolean onCycleResult(long completedCycle, int result) {
//        logger.trace("result(" + completedCycle +")="+ result);
        while (true) {
            ByteTrackerExtent extent = this.markingExtents.get();

//            // Await markable extents, only done by setup and the tracker side
//            if (extent == null) {
//                logger.trace("MARKER>: extent unset, awaiting (for " + completedCycle + ":" + result + ")");
//                while (extent == null) {
//                    try {
////                        report();
////                        logger.trace("MARKER>: acquiring lock to await marking");
//                        lock.lock();
////                        logger.trace("MARKER>: acquired lock, awaiting marking");
//                        nowMarking.await(10, TimeUnit.SECONDS);
////                        logger.trace("MARKER>: awaited marking signal");
//                        lock.unlock();
//                    } catch (InterruptedException ignored) {
//                    }
//                    extent = this.markingExtents.get();
//                }
//                logger.trace("MARKER>: extent set, proceeding");
//            }

            // attempt to mark cycle and result on all available marking extents
            long unmarked = extent.markResult(completedCycle, result);

            if (unmarked < 0) {
                logger.trace("MARKER>: Overmarking attempt, awaiting marking signal");
                try {
                    logger.trace("MARKER>: acquiring lock to await marking");
                    lock.lock();
                    logger.trace("MARKER>: acquired lock");
                    nowMarking.await(10, TimeUnit.SECONDS);
                    logger.trace("MARKER>: awaited marking signal");
                    lock.unlock();
                } catch (InterruptedException ignored) {
                }
                continue;
            }

            if (unmarked == 0) { // If there were ZERO remaining unmarked cycles in all active extents
                try {
                    this.mutex.acquire();

                    // verify that this thread was the same one to observe fully filled
                    if (extent == this.markingExtents.get()) {
                        this.onFullyFilled(extent);
                        // setting the TRACKING extent to the head MARKING extent IFF all marking extents are filled
                        // and no tracking extents are set. Otherwise, do nothing.
                        logger.trace("activating tracking extents " + null + " -> " + extent);
                        if (trackingExtents.compareAndSet(null, extent)) {
//                            report();
                            logger.debug("MARKER>: now tracking extent:" + extent);
//                            logger.trace("MARKER>: acquiring lock to signal tracking");
                            lock.lock();
//                            logger.trace("MARKER>: acquired lock");
                            nowTracking.signalAll();
//                            logger.trace("MARKER>: signaled now tracking " + extent);
                            lock.unlock();
                        }
                    }

//                    // advance the first marking extent to the next available
//                    // This includes the case in which the next extent is null, which is
//                    // effectively the same as disabling marking until the head
//                    // marking extent is defined again.
//
//                    // markingExtents.get().extend();
//
//                    if (!markingExtents.compareAndSet(extent, extent.getNextExtent().get())) {
//                        throw new RuntimeException("MARKER>: Unable to advance marking extent to next extent.");
//                    }
////                    report();
////                    logger.trace("MARKER>: acquiring lock to signal marking");
//                    lock.lock();
////                    logger.trace("MARKER>: acquired lock");
//                    nowMarking.signalAll();
////                    logger.trace("MARKER>: signaled now marking");
//                    lock.unlock();
////                        nowMarking.await(TimeUnit.SECONDS,10);; // else lost the race, just parking, waiting for more extents to write to
//                    this.mutex.release();
                } catch (InterruptedException ignored) {
                }
                return true;
            }
        }

    }

    private void onFullyFilled(ByteTrackerExtent extent) {
        logger.debug("MARKER>: fully filled: " + extent);
    }

    public CycleSegment getSegment(int stride) {
        CycleSegment segment = null;

        while (true) {

            ByteTrackerExtent extent = trackingExtents.get();
            if (extent == null) {
                logger.trace("TRACKER: extent unset, awaiting");

                while (extent == null) {
                    try {
//                        report();
//                        logger.trace("TRACKER: acquiring lock to await tracking");
                        lock.lock();
//                        logger.trace("TRACKER: acquired lock");
                        nowTracking.await(10, TimeUnit.SECONDS);
//                        logger.trace("TRACKER: awaited now marking");
                        lock.unlock();
                    } catch (InterruptedException ignored) {
                    }
                    extent = trackingExtents.get();
                }
                logger.trace("TRACKER: extent set, proceeding");
            }

            segment = extent.getSegment(stride);
            if (segment != null) {
                return segment;
            } else {
                if (extent.isFullyServed()) {
                    try {
                        mutex.acquire();
                        // verify that this thread was the same one to observe fully served
                        if (trackingExtents.get() == extent) {
                            onFullyServed(extent);

                            // Set tracking extent to next defined and filled extent, or null otherwise
                            ByteTrackerExtent nextExtent = extent.getNextExtent().get();
                            if (nextExtent != null && !nextExtent.isFullyFilled()) {
                                nextExtent = null;
                            }
                            logger.trace("swapping tracking extents " + extent + " -> " + nextExtent);
                            if (!trackingExtents.compareAndSet(extent, nextExtent)) {
                                throw new RuntimeException("TRACKER: exclusive access but CAS failed??");
                            }
                            // if not null, notify other threads to continue reading
                            if (trackingExtents.get() != null) {
//                                report();
//                                logger.trace("TRACKER: acquiring lock to signal tracking");
                                lock.lock();
//                                logger.trace("TRACKER: acquired lock");
                                nowTracking.signalAll();
//                                logger.trace("TRACKER: signaled now tracking");
                                lock.unlock();
                            }

                            // when we complete reading an extent, always add another marking extent.
                            logger.trace("TRACKER: Adding marking extent");
                            markingExtents.get().extend();
                            lock.lock();
                            nowMarking.signalAll();
                            lock.unlock();
                        } // else Lost the race, just parking, waiting for more extents to read from
                        mutex.release();

                    } catch (InterruptedException ignored) {
                    }
                } else {
                    throw new RuntimeException("TRACKER: extent was not fully tracked and was unable to return a segment:" + extent);
                }
            }
        }
    }

   public AtomicLong getMinCycle() {
        return min;
    }

    public AtomicLong getMaxCycle() {
        return max;
    }

    public long getPendingCycle() {
        return 0;
    }

    public long getCycleInterval(int stride) {
        // TODO: Rebuild this method separate from getSegment for efficiency
        return getSegment(stride).cycle;
    }

    private void report() {
        logger.trace(" tracker->" + trackingExtents.get());
        logger.trace(" marker>->" + trackingExtents.get());
    }

}
