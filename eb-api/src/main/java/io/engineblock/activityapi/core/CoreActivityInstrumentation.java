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
import com.codahale.metrics.Timer;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.metrics.ActivityMetrics;

public class CoreActivityInstrumentation implements ActivityInstrumentation {

    private static final String STRICTMETRICNAMES = "strictmetricnames";

    private static final String WAIT_TIME = ".waittime";
    private static final String SERVICE_TIME = ".servicetime";
    private static final String RESPONSE_TIME = ".responsetime";

    private final Activity activity;
    private final ActivityDef def;
    private final ParameterMap params;
    private final String svcTimeSuffix;

    public CoreActivityInstrumentation(Activity activity) {
        this.activity = activity;
        this.def = activity.getActivityDef();
        this.params = def.getParams();
        svcTimeSuffix = params.getOptionalBoolean(STRICTMETRICNAMES).orElse(true) ? SERVICE_TIME : "";
    }


    @Override
    public synchronized Timer getOrCreateInputTimer() {
        String metricName = "read_input";
        return ActivityMetrics.timer(def, metricName);
    }


    @Override
    public synchronized Timer getOrCreateStridesServiceTimer() {
        return ActivityMetrics.timer(def, "strides" + SERVICE_TIME);
    }

    @Override
    public synchronized Timer getStridesResponseTimerOrNull() {
        if (activity.getStrideLimiter()==null) {
            return null;
        }
        return ActivityMetrics.timer(def, "strides" + RESPONSE_TIME);
    }


    @Override
    public synchronized Timer getOrCreateCyclesServiceTimer() {
        return ActivityMetrics.timer(def, "cycles" + SERVICE_TIME);
    }

    @Override
    public synchronized Timer getCyclesResponseTimerOrNull() {
        if (activity.getCycleLimiter()==null) {
            return null;
        }
        String metricName = "cycles" + RESPONSE_TIME;
        return ActivityMetrics.timer(def, metricName);
    }


    @Override
    public synchronized Timer getOrCreatePhasesServiceTimer() {
        return ActivityMetrics.timer(def, "phases" + SERVICE_TIME);
    }
    @Override
    public synchronized Timer getPhasesResponseTimerOrNull() {
        if (activity.getPhaseLimiter()==null) {
            return null;
        }
        return ActivityMetrics.timer(def,"phases" + RESPONSE_TIME);
    }

    @Override
    public synchronized Counter getOrCreatePendingOpCounter() {
        String metricName = "pending_ops";
        return ActivityMetrics.counter(def, metricName);
    }

}
