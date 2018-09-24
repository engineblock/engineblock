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

package io.engineblock.activityapi.core;

import com.codahale.metrics.Counter;
import io.engineblock.activityapi.core.ops.OpContext;
import io.engineblock.activityapi.core.ops.fluent.TrackedOp;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.motor.AsyncTracker;
import io.engineblock.metrics.ActivityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAsyncAction<D, A extends Activity> implements AsyncAction<D>, Stoppable, ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger("BaseAsyncAction");

    protected final AsyncTracker tracker;
    protected final A activity;

    public Counter pendingOpsCounter;
    protected int slot;
    protected boolean running = true;

    public BaseAsyncAction(A activity, int slot) {
        this.activity = activity;
        this.slot = slot;
        onActivityDefUpdate(activity.getActivityDef());
        pendingOpsCounter = ActivityMetrics.counter(activity.getActivityDef(), "pending_ops");
        tracker = new AsyncTracker(pendingOpsCounter);
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        ParameterMap params = activityDef.getParams();
        params.getOptionalInteger("async").orElseThrow(
                () -> new RuntimeException("the async parameter is required to activate async actions"));
        this.tracker.setMaxOps(getMaxPendingOps(activityDef));
    }

    protected int getMaxPendingOps(ActivityDef def) {
        int maxTotalOpsInFlight = def.getParams().getOptionalInteger("async").orElse(1);
        int threads = def.getThreads();
        return (maxTotalOpsInFlight / threads) + (slot < (maxTotalOpsInFlight % threads) ? 1 : 0);
    }

    public boolean enqueue(TrackedOp opc) {
        synchronized (this) {
            while (tracker.isFull()) {
                try {
                    logger.trace("Blocking for enqueue with (" + tracker.getPendingOps() + "/" + tracker.getMaxOps() + ") queued ops");
                    tracker.wait(60000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        startOpCycle(opc);
        return (running && !tracker.isFull());
    }

    @Override
    public synchronized boolean awaitCompletion(long timeout) {
        long endAt = System.currentTimeMillis() + timeout;
        while (running && tracker.getPendingOps() > 0 && System.currentTimeMillis() < endAt) {
            try {
                long waitfor = Math.max(0, endAt - System.currentTimeMillis());
                wait(waitfor);
            } catch (InterruptedException ignored) {
            }
        }
        return tracker.getPendingOps() == 0;
    }

    /**
     * Implementations that extend this base class can call this method in order to put
     * an operation in flight.
     *
     * @param opc The type-specific {@link OpContext}
     */
    abstract void startOpCycle(TrackedOp opc);

    @Override
    public void requestStop() {
        logger.info(this.toString() + " requested to stop.");
        this.running = false;
    }

}
