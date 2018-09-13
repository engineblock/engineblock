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
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.metrics.ActivityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAsyncAction<T extends OpContext, A extends Activity> implements AsyncAction<T>, Stoppable, ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger("BaseAsyncAction");

    protected final A activity;
    public Counter pendingOpsCounter;
    protected int slot;
    protected boolean running = true;

    private int pendingOpsQueuedForThread = 0;
    private int maxOpsQueuedForThread = 1;

    public BaseAsyncAction(A activity, int slot) {
        this.activity = activity;
        this.slot = slot;
        onActivityDefUpdate(activity.getActivityDef());
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        ParameterMap params = activityDef.getParams();
        params.getOptionalInteger("async").orElseThrow(
                () -> new RuntimeException("the async parameter is required to activate async actions"));
        this.maxOpsQueuedForThread = getMaxPendingOps(activityDef);
        pendingOpsCounter = ActivityMetrics.counter(activityDef, "pending_ops");
    }

    protected int getMaxPendingOps(ActivityDef def) {
        int maxTotalOpsInFlight = def.getParams().getOptionalInteger("async").orElse(1);
        int threads = def.getThreads();
        return (maxTotalOpsInFlight / threads) + (slot < (maxTotalOpsInFlight % threads) ? 1 : 0);
    }

    @Override
    public boolean enqueue(T opc) {
        synchronized (this) {
            while (available() == 0) {
                try {
                    logger.trace("Blocking for enqueue with (" + pendingOpsQueuedForThread + "/" + maxOpsQueuedForThread + ") queued ops");
                    wait(60000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        incrementOps();
        startOpCycle(opc);
        return (running && available() > 0);
    }

    @Override
    public synchronized boolean awaitCompletion(long timeout) {
        long endAt = System.currentTimeMillis() + timeout;
        while (running && pending() > 0 && System.currentTimeMillis() < endAt) {
            try {
                long waitfor = Math.max(0, endAt - System.currentTimeMillis());
                wait(waitfor);
            } catch (InterruptedException ignored) {
            }
        }
        return pending() == 0;
    }

    protected int available() {
        return maxOpsQueuedForThread - pendingOpsQueuedForThread;
    }
    protected int pending() {
        return pendingOpsQueuedForThread;
    }

    protected void incrementOps() {
        this.pendingOpsCounter.inc();
        pendingOpsQueuedForThread++;
    }

    protected void decrementOps() {
        this.pendingOpsCounter.dec();
        pendingOpsQueuedForThread--;
        if (pendingOpsQueuedForThread == 0 || pendingOpsQueuedForThread == (maxOpsQueuedForThread - 1)) {
            // Either waiting for not full (enqueue), or waiting for empty (awaitCompletion)
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Implementations that extend this base class can call this method in order to put
     * an operation in flight.
     *
     * @param opc The type-specific {@link OpContext}
     * @return the type-specific {@link OpContext}
     */
    protected abstract T startOpCycle(T opc);

    @Override
    public void requestStop() {
        logger.info(this.toString() + " requested to stop.");
        this.running = false;
    }

    @Override
    public abstract T newOpContext();
}
