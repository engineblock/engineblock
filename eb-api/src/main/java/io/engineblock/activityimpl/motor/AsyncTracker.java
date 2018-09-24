package io.engineblock.activityimpl.motor;

import com.codahale.metrics.Counter;
import io.engineblock.activityapi.core.ops.OpContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The async tracker provides a junction point for registering listeners
 * or event handlers on operational events as they occur within a thread.
 * It also makes it simpler for async action implementations to manage
 * concurrency limits. This type is exposed via the async action interface
 * so that motors can manage callbacks that are essential for tracking
 * strides.
 */
public class AsyncTracker {
    private final AtomicInteger pendingOps = new AtomicInteger(0);
    private int maxOps =0;
    private final Counter pendingOpsCounter;

    public AsyncTracker(Counter pendingOpsCounter) {
        this.pendingOpsCounter = pendingOpsCounter;
    }

    public void setMaxOps(int maxOps) {
        this.maxOps = maxOps;
    }

    public Tracked start(OpContext opc) {
        opc.start();
        pendingOps.incrementAndGet();
        pendingOpsCounter.inc();
        return new Tracked(opc);
    }

    void stop(OpContext opc) {
        pendingOps.decrementAndGet();
        pendingOpsCounter.dec();
        int pending = this.pendingOps.decrementAndGet();

        if (pending<maxOps) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    Tracked register(OpContext opc) {
        return new Tracked(opc);
    }

    public int getPendingOps() {
        return pendingOps.intValue();
    }

    public int getMaxOps() {
        return maxOps;
    }

    public boolean isFull() {
        return this.pendingOps.intValue()>=maxOps;
    }

    public static class Tracked<O extends OpContext>  {
        public final O obj;
        protected Tracked(O opc) {
            obj = opc;
        }
    }

}
