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
    private final AtomicLong maxContiguousValue;
    private final AtomicLong pendingValue;
    private AtomicLong min;
    private AtomicLong max;
    //    private AtomicReference<ByteTrackerExtent[]> extents;
    private Semaphore aryShuffler = new Semaphore(1);
    private AtomicReference<ByteTrackerExtent> servingExtent = new AtomicReference<>();
    private AtomicReference<ByteTrackerExtent> markingExtent = new AtomicReference<>();
    private AtomicReference<ByteTrackerExtent> tailExtent = new AtomicReference<>();
    private final ReentrantLock bufferLock = new ReentrantLock(false);
    private final Condition markerBlocked = bufferLock.newCondition();

    public CoreTracker(long min, long max, int extentSize) {
        this.min = new AtomicLong(min);
        this.max = new AtomicLong(max);
        this.maxContiguousValue = new AtomicLong(min - 1);
        this.pendingValue = new AtomicLong(min);

        this.extentSize = extentSize;
        markingExtent.set(new ByteTrackerExtent(min, (min+extentSize)-1));
        servingExtent.set(markingExtent.get());
        tailExtent.set(markingExtent.get());

    }

    @Override
    public CycleSegment getSegment(int stride) {
        CycleSegment segment = null;
        while (true) {
            segment = servingExtent.get().getSegment(stride); // will block if logically able to serve a result
            if (segment != null) {
                return segment;
            } else {
                rotateExtents(); // not logically able to serve a result
            }
        }
    }

//            long p = pendingValue.get();
//            if (p + stride <= maxContiguousValue.get()) {
//                if (pendingValue.compareAndSet(p, p + stride)) {
//                    CycleSegment s = new CycleSegment();
//                    s.cycle =p+stride;
//                    return s;
//                } // else CAS failed, try again, another thread beat us to it
//            } else if (markingExtents.get().isFilled()) {
//                bufferSwap("get stride");
//            } // else we're here too early, let's try again
//        }

    public long getCycleInterval(int stride) {
        // TODO: Rebuild this method separate from getSegment for efficiency
        return getSegment(stride).cycle;
    }

//    public long[] getCycleValues(long stride) {
//    }

    @Override
    public boolean markResult(long completedCycle, int result) {
        while (true) {
            ByteTrackerExtent extent = this.markingExtent.get();
            while (extent != null) {
                if (extent.markResult(completedCycle, result)) {
                    return true;
                }
                extent = extent.getNextExtent().get();
            }
            // If you got to here, then nothing would accept the mark.
            // Add another extent and try again, or block and wait for readers to catch up.

            try {
                synchronized(this) {
                    logger.info("marker waiting for readers to catch up.");
                    this.wait(1000);
                    logger.info("marker awoken.");
                }
            } catch (InterruptedException ignored) {
            }
        }

    }

    private void rotateExtents() {
        ByteTrackerExtent servingBefore = servingExtent.get();
        try {
            aryShuffler.acquire();
            if (servingBefore == servingExtent.get()) {
                if (servingBefore.isFullyServed()) {

                    ByteTrackerExtent nextExtent = servingBefore.getNextExtent().get();
                    if (nextExtent == null) {
                        long nextMin = servingBefore.getMinCycle().get() + extentSize;
                        long nextMax = servingBefore.getMaxCycle().get() + extentSize;
                        nextExtent = new ByteTrackerExtent(nextMin, nextMax);
                    }
                    onCompletedExtent(servingBefore);
                    if (!servingExtent.compareAndSet(servingBefore, nextExtent)) {
                        throw new RuntimeException("Exclusive access via semaphore, but CAS fails???");
                    }
                    // Unpark all waiting monitors
                    synchronized (this) {
                        logger.info("notifying blocked tracking threads.");
                        this.notifyAll();
                    }


                } else {
                    throw new RuntimeException("rotating extents that aren't fully served.");
                }
            } else {
                logger.debug("lost the race");
            }
            aryShuffler.release();

        } catch (InterruptedException ignored) {
        }
    }

//    /**
//     * This method handles atomically rotating buffers. It should be called any time
//     * either a source or a sink sees a state for which it can't advance.
//     */
//    private void bufferSwap(String threadRole) {
//        ByteTrackerExtent[] byteTrackerExtents = this.extents.get();
//        try {
//            aryShuffler.acquire();
//            // The operative marking thread will see the reference unchanged,
//            // All others (the ones parked in acquire in the mean time) will see it changed
//            if (byteTrackerExtents == this.extents.get()) {
//                logger.debug(threadRole + " entering main bufferSwap");
//
//                ByteTrackerExtent extent0 = extents.get()[0];
//                boolean bucket0filled = extent0.isFilled();
//                boolean bucket0usedup = extent0.getMaxCycle().get() <= pendingValue.get();
//                if (bucket0filled && bucket0usedup) {
//                    ByteTrackerExtent[] newTrackers = new ByteTrackerExtent[]{
//                            byteTrackerExtents[1],
//                            byteTrackerExtents[2],
//                            new ByteTrackerExtent(byteTrackerExtents[2].getMaxCycle().get() + 1,
//                                    byteTrackerExtents[2].getMaxCycle().get() + extentSize)};
//                    onCompletedExtent(byteTrackerExtents[0]);
//                    if (!this.extents.compareAndSet(byteTrackerExtents, newTrackers)) {
//                        throw new RuntimeException("Mutual access via semaphore, but CAS fails??");
//                    }
//                } else {
//                    logger.debug(
//                            "errorneous blocking, this needs to be fixed. bucket0filled="
//                                    + bucket0filled + ", usedup=" + bucket0usedup);
//                    Thread.sleep(250);
//
////                    int sleeptill = 1000000;
////                    while (!this.extents.get()[0].isFilled()) {
////                        Thread.sleep(sleeptill / 1000000, sleeptill % 1000000);
////                        logger.debug(threadRole + " awaiting fill on " + this.extents.get()[0] + " for " + sleeptill + "ns");
////                        sleeptill = Math.max(sleeptill * 2, 100000000);
////                    }
////                    while (pendingValue.get()<=(this.extents.get()[0].getMaxCycle().get())) {
////                        Thread.sleep(sleeptill / 1000000, sleeptill % 1000000);
////                        logger.debug(threadRole + " awaiting dispatched on " + this.extents.get()[0] + " for " + sleeptill + "ns");
////                        sleeptill = Math.max(sleeptill * 2, 100000000);
////                    }
//
//                }
//                // Await the first extent being fully completed before retiring it.
//                // This only happens in the operative thread, after which other threads
//                // are unparked and allowed to see the state change
//
//            } else {
//                logger.debug(threadRole + " bypassed main bufferSwap");
//            }
//            aryShuffler.release();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//    }

    private void onCompletedExtent(ByteTrackerExtent byteTrackerExtent) {
        logger.debug("retiring extent: " + byteTrackerExtent);
        maxContiguousValue.addAndGet(extentSize);
    }


    @Override
    public boolean isCycleCompleted(long cycle) {
        return (maxContiguousValue.get() >= cycle);
    }

    @Override
    public long getMaxContiguousMarked() {
        return maxContiguousValue.get();
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
        return pendingValue.get();
    }

}
