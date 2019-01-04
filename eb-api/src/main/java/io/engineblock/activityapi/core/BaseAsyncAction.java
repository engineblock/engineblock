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
import io.engineblock.activityapi.core.ops.fluent.opcontext.OpContext;
import io.engineblock.activityapi.core.ops.fluent.OpTracker;
import io.engineblock.activityapi.core.ops.fluent.OpTrackerImpl;
import io.engineblock.activityapi.core.ops.fluent.opfacets.TrackedOp;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.metrics.ActivityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @param <D> An type of state holder for an operation, holding everything unique to that cycle and operation
 * @param <A> An type of of an Activity, a state holder for a runtime instance of an Activity
 */
public abstract class BaseAsyncAction<D, A extends Activity> implements AsyncAction<D>, Stoppable, ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger("BaseAsyncAction");

    protected final OpTracker<D> tracker;
    protected final A activity;

    public Counter pendingOpsCounter;
    protected int slot;
    protected boolean running = true;

    public BaseAsyncAction(A activity, int slot) {
        this.activity = activity;
        this.slot = slot;
        pendingOpsCounter = ActivityMetrics.counter(activity.getActivityDef(), "pending_ops");
        boolean enableCOMetrics= (activity.getCycleLimiter()!=null);

        tracker = new OpTrackerImpl<>(activity, slot);
        onActivityDefUpdate(activity.getActivityDef());
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        ParameterMap params = activityDef.getParams();
        params.getOptionalInteger("async").orElseThrow(
                () -> new RuntimeException("the async parameter is required to activate async actions"));
        this.tracker.setMaxPendingOps(getMaxPendingOpsForThisThread(activityDef));
    }

    protected int getMaxPendingOpsForThisThread(ActivityDef def) {
        int maxTotalOpsInFlight = def.getParams().getOptionalInteger("async").orElse(1);
        int threads = def.getThreads();
        return (maxTotalOpsInFlight / threads) + (slot < (maxTotalOpsInFlight % threads) ? 1 : 0);
    }

    public boolean enqueue(TrackedOp<D> opc) {
        synchronized (tracker) {
            while (tracker.isFull()) {
                try {
                    logger.trace("Blocking for enqueue with (" + tracker.getPendingOps() + "/" + tracker.getMaxPendingOps() + ") queued ops");
                    tracker.wait(10000);
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
    public abstract void startOpCycle(TrackedOp<D> opc);

//    public abstract CompletedOp<D> completeOpCycle(StartedOp<D> opc) {}

    @Override
    public void requestStop() {
        logger.info(this.toString() + " requested to stop.");
        this.running = false;
    }

    @Override
    public OpTracker<D> getTracker() {
        return tracker;
    }
}
