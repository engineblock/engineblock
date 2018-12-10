package io.engineblock.activityapi.core.ops.fluent;

import com.codahale.metrics.Counter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This tracker keeps dibs on the state of operations associated with it.
 *
 * @param <D> The payload data type of the associated Op, based on OpImpl
 */
public class OpTrackerImpl<D> implements OpTracker<D> {
    private final AtomicInteger pendingOps = new AtomicInteger(0);
    private final Counter pendingOpsCounter;
    private int maxPendingOps =1;


    public OpTrackerImpl(Counter pendingOpsCounter) {
        this.pendingOpsCounter = pendingOpsCounter;
    }
    
    @Override
    public void onStarted(StartedOp<D> op) {
        pendingOps.incrementAndGet();
        pendingOpsCounter.inc();
    }

    public void onCompleted(CompletedOp<D> op) {
        pendingOpsCounter.dec();
        int pending = this.pendingOps.decrementAndGet();

        if (pending< maxPendingOps) {
            synchronized (this) {
                notify();
            }
        }
    }

    @Override
    public void setMaxPendingOps(int maxPendingOps) {
        this.maxPendingOps =maxPendingOps;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public boolean isFull() {
        return this.pendingOps.intValue()>=maxPendingOps;
    }

    @Override
    public int getPendingOps() {
        return pendingOps.intValue();
    }

    public int getMaxPendingOps() {
        return maxPendingOps;
    }
}
