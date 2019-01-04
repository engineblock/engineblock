package io.engineblock.activityapi.core.ops.fluent;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.Activity;
import io.engineblock.activityapi.core.ops.fluent.opfacets.CompletedOp;
import io.engineblock.activityapi.core.ops.fluent.opfacets.StartedOp;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This tracker keeps track of the state of operations associated with it.
 *
 * @param <D> The payload data type of the associated Op, based on OpImpl
 */
public class OpTrackerImpl<D> implements OpTracker<D> {
    private final AtomicInteger pendingOps = new AtomicInteger(0);
    private final String label;
    private final int slot;
    private final Timer cycleServiceTimer;
    private final Timer cycleResponseTimer;
    private final Counter pendingOpsCounter;

    private int maxPendingOps =1;


    public OpTrackerImpl(Activity activity, int slot) {
        this.slot = slot;
        this.label = "tracker-" + slot + "_" + activity.getAlias();

        this.pendingOpsCounter = activity.getInstrumentation().getOrCreatePendingOpCounter();
        this.cycleServiceTimer = activity.getInstrumentation().getOrCreateCyclesServiceTimer();
        this.cycleResponseTimer = activity.getInstrumentation().getCyclesResponseTimerOrNull();
    }

    // for testing
    public OpTrackerImpl(String name, int slot, Timer cycleServiceTimer, Timer cycleResponseTimer, Counter pendingOpsCounter) {
        this.label = name;
        this.slot = slot;
        this.cycleResponseTimer = cycleResponseTimer;
        this.cycleServiceTimer = cycleServiceTimer;
        this.pendingOpsCounter = pendingOpsCounter;
    }

    @Override
    public void onStarted(StartedOp<D> op) {
        pendingOps.incrementAndGet();
        pendingOpsCounter.inc();
    }

    @Override
    public void onCompleted(CompletedOp<D> op) {
        pendingOpsCounter.dec();
        int pending = this.pendingOps.decrementAndGet();

        cycleServiceTimer.update(op.getServiceTime(), TimeUnit.NANOSECONDS);
        if (cycleResponseTimer !=null) { cycleResponseTimer.update(op.getResponseTime(), TimeUnit.NANOSECONDS); }

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

    @Override
    public String toString() {
        return "OpTracker-" + label + ":" + this.slot + " " + this.pendingOps.get() + "/" + maxPendingOps + " ops ";
    }
}
