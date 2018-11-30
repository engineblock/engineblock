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
package io.engineblock.activities.diag;

import io.engineblock.activityapi.core.BaseAsyncAction;
import io.engineblock.activityapi.core.ops.BaseOpContext;
import io.engineblock.activityapi.core.ops.OpContext;
import io.engineblock.activityapi.ratelimits.RateLimiter;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;

public class AsyncDiagAction extends BaseAsyncAction<OpContext, DiagActivity> {

    private final static Logger logger = LoggerFactory.getLogger(AsyncDiagAction.class);

    private long lastUpdate;
    private long quantizedInterval;
    private long reportModulo;
    private int phasesPerCycle;
    private int completedPhase;
    private int resultmodulo = Integer.MIN_VALUE;
    private long erroroncycle = Long.MIN_VALUE;
    private long throwoncycle = Long.MIN_VALUE;
    private boolean logcycle;
    private int staticvalue = Integer.MIN_VALUE;
    private RateLimiter diagRateLimiter = null;

    private ArrayDeque<OpContext> asyncOps;


    public AsyncDiagAction(DiagActivity activity, int slot) {
        super(activity, slot);
        onActivityDefUpdate(activity.getActivityDef());
    }

    /**
     * assign the last append reference time and the interval which, when added to it, represent when this
     * diagnostic thread should take its turn to log cycle info. Also, append the modulo parameter.
     */
    private void updateReportTime() {
        ParameterMap params = this.activity.getActivityDef().getParams();
        reportModulo = params.getOptionalLong("modulo").orElse(10000000L);
        lastUpdate = System.currentTimeMillis() - calculateOffset(slot, params);
        quantizedInterval = calculateInterval(params, activity.getActivityDef().getThreads());
        logger.trace("updating report time for slot:" + slot + ", def:" + params + " to " + quantizedInterval
                + ", and modulo " + reportModulo);
    }

    /**
     * Calculate a reference point in the past which would have been this thread's time to append,
     * for use as a discrete reference point upon which the quantizedIntervals can be stacked to find the
     * ideal schedule.
     *
     * @param timeslot - This thread's offset within the scheduled rotation, determined simply by thread enumeration
     * @param params   - the def for this activity instance
     * @return last time this thread would have updated
     */
    private long calculateOffset(long timeslot, ParameterMap params) {
        long updateInterval = params.getOptionalLong("interval").orElse(1000L);
        long offset = calculateInterval(params, activity.getActivityDef().getThreads()) - (updateInterval * timeslot);
        return offset;
    }

    /**
     * Calculate how frequently a thread needs to append in order to achieve an aggregate append interval for
     * a given number of cooperating threads.
     *
     * @param params - the def for this activity instance
     * @return long ms interval for this thread (the same for all threads, but calculated independently for each)
     */
    private long calculateInterval(ParameterMap params, int threads) {
        long updateInterval = params.getOptionalLong("interval").orElse(1000L);
        if (updateInterval == 0) { // Effectively disable this if it is set to 0 as an override.
            return Long.MAX_VALUE;
        }

        return updateInterval * threads;
    }


    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        super.onActivityDefUpdate(activityDef);

        ParameterMap params = activityDef.getParams();
        updateReportTime();

        this.resultmodulo = params.getOptionalInteger("resultmodulo").orElse(Integer.MIN_VALUE);
        this.erroroncycle = params.getOptionalLong("erroroncycle").orElse(Long.MIN_VALUE);
        this.throwoncycle = params.getOptionalLong("throwoncycle").orElse(Long.MIN_VALUE);
        this.logcycle = params.getOptionalBoolean("logcycle").orElse(false);
        this.staticvalue = params.getOptionalInteger("staticvalue").orElse(-1);
        this.diagRateLimiter = activity.getDiagRateLimiter();
        this.asyncOps = new ArrayDeque<>(this.getMaxPendingOps(activityDef));
    }

    @Override
    public boolean enqueue(OpContext opc) {
        if (available()==0) {
            finishOpCycle();
        }
        return super.enqueue(opc);
    }

    @Override
    protected OpContext startOpCycle(OpContext opc) {
        opc.start();
        this.asyncOps.addLast(opc);
        return opc;
    }

    private void finishOpCycle() {
        OpContext opc = asyncOps.removeFirst();
        opc.stop(runCycle(opc.getCycle()));
        decrementOps();
    }


    private int runCycle(long cycle) {

        if (logcycle) {
            logger.trace("cycle " + cycle);
        }

        if (diagRateLimiter != null) {
            diagRateLimiter.maybeWaitForOp();
        }

        long now = System.currentTimeMillis();
        if (completedPhase >= phasesPerCycle) {
            completedPhase = 0;
        }

        if ((now - lastUpdate) > quantizedInterval) {
            long delay = ((now - lastUpdate) - quantizedInterval);
            logger.info("diag action interval, input=" + cycle + ", phase=" + completedPhase + ", report delay=" + delay + "ms");
            lastUpdate += quantizedInterval;
            activity.delayHistogram.update(delay);
        }

        if ((cycle % reportModulo) == 0) {
            logger.info("diag action   modulo, input=" + cycle + ", phase=" + completedPhase);
        }

        completedPhase++;

        int result = 0;

        if (resultmodulo >= 0) {
            if ((cycle % resultmodulo) == 0) {
                result = 1;
            }
        } else if (staticvalue >= 0) {
            return staticvalue;
        } else {
            result = (byte) (cycle % 128);
        }

        if (erroroncycle == cycle) {
            activity.getActivityController().stopActivityWithReasonAsync("Diag was requested to stop on cycle " + erroroncycle);
        }

        if (throwoncycle == cycle) {
            throw new RuntimeException("Diag was asked to throw an error on cycle " + throwoncycle);
        }

        return result;
    }

    @Override
    public boolean awaitCompletion(long timeout) {
        while (pending()>0) {
            finishOpCycle();
        }
        return true; // Diag action doesn't have any asynchronous logic to demonstrate
    }

    @Override
    public DiagOpContext newOpContext() {
        return new DiagOpContext("This is a diag op context.");
    }

    public static class DiagOpContext extends BaseOpContext {
        private String description;

        DiagOpContext(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return super.toString() + ", description:'" + description;
        }
    }
}
