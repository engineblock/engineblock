/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package io.engineblock.activities.diag;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActivityDefObserver;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagAction implements Action, ActivityDefObserver {

    private final static Logger logger = LoggerFactory.getLogger(DiagAction.class);
    private final ActivityDef activityDef;
    private final DiagActivity diagActivity;

    private int slot;
    private long lastUpdate;
    private long quantizedInterval;
    private long reportModulo;

    public DiagAction(int slot, ActivityDef activityDef, DiagActivity diagActivity) {
        this.activityDef = activityDef;
        this.slot = slot;
        this.diagActivity = diagActivity;

        updateReportTime();
    }

    @Override
    public void accept(long value) {
        long now = System.currentTimeMillis();
        if ((now - lastUpdate) > quantizedInterval) {
            long delay = ((now - lastUpdate) - quantizedInterval);
            logger.info("diag action interval, input=" + value + ", report delay=" + delay);
            lastUpdate += quantizedInterval;
            diagActivity.delayHistogram.update(delay);
        }
        if ((value % reportModulo) == 0) {
            logger.info("diag action   modulo, input=" + value);
        }
    }

    /**
     * idempotently assign the last update reference time and the interval which, when added to it, represent when this
     * diagnostic thread should take its turn to log cycle info. Also, update the modulo parameter.
     */
    private void updateReportTime() {

        reportModulo = activityDef.getParams().getLongOrDefault("modulo", 10000000);
        lastUpdate = System.currentTimeMillis() - calculateOffset(slot, activityDef);
        quantizedInterval = calculateInterval(activityDef);
        logger.trace("updating report time for slot:" + slot + ", def:" + activityDef + " to " + quantizedInterval
                + ", and modulo " + reportModulo);

    }

    /**
     * Calculate a reference point in the past which would have been this thread's time to update,
     * for use as a discrete reference point upon which the quantizedIntervals can be stacked to find the
     * ideal schedule.
     * @param timeslot - This thread's offset within the scheduled rotation, determined simply by thread enumeration
     * @param activityDef - the def for this activity instance
     * @return last time this thread would have updated
     */
    private long calculateOffset(long timeslot, ActivityDef activityDef) {
        long updateInterval = activityDef.getParams().getLongOrDefault("interval", 1000L);
        long offset = calculateInterval(activityDef) - (updateInterval * timeslot);
        return offset;
    }

    /**
     * Calculate how frequently a thread needs to update in order to achieve an aggregate update interval for
     * a given number of cooperating threads.
     * @param activityDef - the def for this activity instance
     * @return long ms interval for this thread (the same for all threads, but calculated independently for each)
     */
    private long calculateInterval(ActivityDef activityDef) {
        long updateInterval = activityDef.getParams().getLongOrDefault("interval", 1000L);
        if (updateInterval == 0) { // Effectively disable this if it is set to 0 as an override.
            return Long.MAX_VALUE;
        }

        int threads = activityDef.getThreads();
        return updateInterval * threads;
    }


    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        updateReportTime();
    }
}
