package io.engineblock.activityimpl.tracker;

import io.engineblock.activityapi.cycletracking.CycleSegment;
import io.engineblock.activityapi.cycletracking.CycleSinkSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RingTracker implements CycleSinkSource {

    private final static Logger logger = LoggerFactory.getLogger(RingTracker.class);

    private final AtomicLong minCycle = new AtomicLong(0L);
    private final AtomicLong maxCycle = new AtomicLong(0L);

    private final int bufsize;
    private final int windowSize;

    private AtomicIntegerArray buffer;
    private AtomicLong bufferBase = new AtomicLong(0L); // logical cycle number anchored at buffer position 0
    private AtomicLong markingStart = new AtomicLong(0L); // markingStart of active window as logical cycle number
    private AtomicLong pendingCycle = new AtomicLong(0L);
    private ReentrantLock lock = new ReentrantLock(false);
    private Condition canWrite = lock.newCondition();
    private Condition canRead = lock.newCondition();

    public RingTracker(long min, long max, int bufsize, int windowSize) {
        this.minCycle.set(min);
        this.maxCycle.set(max);
        this.bufsize = bufsize;
        this.windowSize = windowSize;
        if ((((bufsize - windowSize) - 1) - windowSize) < 0) {
            throw new RuntimeException("buffsize must be 1 + (2 x window size) at a minimum.");
        }
        setupBuffer();
        logger.debug("new ring tracker: " + this);
    }

    private void setupBuffer() {
        buffer = new AtomicIntegerArray(bufsize);
        for (int i = 0; i < buffer.length(); i++) {
            buffer.set(i, -1);
        }
        bufferBase.set(minCycle.get());
        markingStart.set(minCycle.get());
        pendingCycle.set(minCycle.get());
    }

    // invariant: once a cycle is markable according to window bounds, it will
    // remain markable until at least that cycle is marked and tabulated

    private int bufferOffset(long completedCycle) {
        long base = bufferBase.get();
        long windowed = completedCycle - base;
        int boffset = (int) (windowed % bufsize);
        return boffset;
    }

    @Override
    public boolean consumeResult(long completedCycle, int result) {
        while (true) {
            long wstart = this.markingStart.get();
            logger.debug("marking result  " + completedCycle + " => " + result);

            // TODO: consider deterministic forward unmarking, i.e. markingStart+windowSize
            if (completedCycle >= wstart && completedCycle < (wstart + windowSize)) {
                if (buffer.compareAndSet(bufferOffset(completedCycle), -1, result)) {

                    long pending = this.pendingCycle.get();
                    // fast forward start to next unmarked region if possible
                    while (
                            completedCycle == markingStart.get()
                                    && buffer.get(bufferOffset(completedCycle)) > 0
                                    && pendingCycle.get() >= (completedCycle - windowSize)

                            ) {
                        if (markingStart.compareAndSet(completedCycle, completedCycle + 1)) {
                            buffer.set(bufferOffset(completedCycle + windowSize), -1);
                            completedCycle++;
                            logger.debug("signalling canWrite & canWrite");
                            lock.lock();
                            canWrite.signalAll();
                            logger.debug("signalled canWrite");
                            canRead.signalAll();
                            logger.debug("signalled canRead");
                            lock.unlock();
                            logger.debug("advanced to " + completedCycle);
                        } else {
                            logger.error("unable to advance to " + completedCycle);
                        }
                    }
                    return true;
                } else {
                    throw new RuntimeException("buffer consistency error: bufferOffset:" +
                            bufferOffset(completedCycle) + " should have been -1 prior");
                }
            } else {
                if (completedCycle < wstart) {
                    throw new RuntimeException("attempted to mark cycle:" + completedCycle + ", but pending starts at " + pendingCycle.get());
                }
                try {
                    logger.debug("awaiting canWrite");
                    lock.lock();
                    boolean awaited = canWrite.await(10, TimeUnit.SECONDS);
                    lock.unlock();
                    logger.debug("awaited canWrite, timeout?==" + !awaited);
                } catch (InterruptedException ignored) {
                }
            }

        }

    }

    @Override
    public CycleSegment getSegment(int stride) {
        while (true) {

            long pending = pendingCycle.get();
            long starting = markingStart.get();
            if (stride > windowSize) {
                throw new RuntimeException("Stride:" + stride + " must be less than or equal to window size:" + windowSize);
            }
            if (pending >= (starting - windowSize) && pending+stride <= starting) {
                if (pendingCycle.compareAndSet(pending, pending + stride)) {
                    int boff = bufferOffset(pending);
                    byte[] data = new byte[stride];
                    for (int i = 0; i < stride; i++) {
                        data[i] = (byte) (127 & buffer.get(boff + i));
                    }
                    return CycleSegment.forData(pending, data);
                }
            } else {
                try {
                    logger.debug("awaiting canRead");
                    lock.lock();
                    boolean awaited = canRead.await(1, TimeUnit.SECONDS);
                    lock.unlock();
                    logger.debug("awaited canRead, timeout?==" + !awaited);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public long getCycleInterval(int stride) {
        return 0;
    }

    @Override
    public long remainingCycles() {
        return maxCycle.get() - markingStart.get();
    }

    @Override
    public AtomicLong getMinCycle() {
        return minCycle;
    }

    @Override
    public AtomicLong getMaxCycle() {
        return maxCycle;
    }

    @Override
    public long getPendingCycle() {
        return markingStart.get();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(minCycle.get()).append(",").append(maxCycle.get()).append("] ");
        if (windowSize < 256) {
            sb.append("start:").append(markingStart.get()).append(" window:|");
            for (int i = 0; i < windowSize; i++) {
                int val = buffer.get(bufferOffset(markingStart.get() + i));
                sb.append(val).append("|");
            }
        }
        return sb.toString();
    }

}
