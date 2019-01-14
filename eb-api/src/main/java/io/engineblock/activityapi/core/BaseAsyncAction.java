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

import io.engineblock.activityapi.core.ops.fluent.opfacets.TrackedOp;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @param <D> An type of state holder for an operation, holding everything unique to that cycle and operation
 * @param <A> An type of of an Activity, a state holder for a runtime instance of an Activity
 */
public abstract class BaseAsyncAction<D, A extends Activity> implements AsyncAction<D>, Stoppable, ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger("BaseAsyncAction");

    protected final A activity;

    protected int slot;
    protected boolean running = true;

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
    }

    public boolean enqueue(TrackedOp<D> opc) {
        startOpCycle(opc);
        return (running);
    }

    /**
     * Implementations that extend this base class can call this method in order to put
     * an operation in flight.
     *
     * @param opc A tracked operation with state of parameterized type D
     */
    public abstract void startOpCycle(TrackedOp<D> opc);

    @Override
    public void requestStop() {
        logger.info(this.toString() + " requested to stop.");
        this.running = false;
    }

}
