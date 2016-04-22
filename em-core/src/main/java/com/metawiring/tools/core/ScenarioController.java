/*
*   Copyright 2015 jshook
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
package com.metawiring.tools.core;

import com.metawiring.tools.activityapi.ActivityType;
import com.metawiring.tools.activityapi.ActivityDef;
import com.metawiring.tools.activityapi.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A ScenarioController provides a way to start Activities, modify them while running, and stop, pause or restart them.
 */
public class ScenarioController {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioController.class);

    private final Map<String, ActivityExecutor> activityExecutors = new HashMap<>();

    public synchronized void start(ActivityDef activityDef) {
        getActivityExecutor(activityDef).start();
    }

    public synchronized void start(String activityDef) {
        start(ActivityDef.parseActivityDef(activityDef));
    }

    public synchronized void stop(ActivityDef activityDef) {
        getActivityExecutor(activityDef).stop();
    }

    public synchronized void stop(String activityDef) {
        stop(ActivityDef.parseActivityDef(activityDef));
    }

    public synchronized void modify(String activityAlias, String param, String value) {
        ActivityExecutor activityExecutor = getActivityExecutor(activityAlias);
        ParameterMap params = activityExecutor.getActivityDef().getParams();
        params.set(param, value);
    }

    /**
     * Get the activity executor associated with the given alias. This should be used to find activitytypes
     * which are presumed to be already defined.
     *
     * @param activityAlias The activity alias for the extant activity.
     * @return the associated ActivityExecutor
     * @throws RuntimeException a runtime exception if the named activity is not found
     */
    private ActivityExecutor getActivityExecutor(String activityAlias) {
        Optional<ActivityExecutor> executor =
                Optional.ofNullable(activityExecutors.get(activityAlias));
        return executor.orElseThrow(
                () -> new RuntimeException("ActivityExecutor for alias " + activityAlias + " not found.")
        );

    }

    // TODO: Move activity construction logic out of scenario controller
    private ActivityExecutor getActivityExecutor(ActivityDef activityDef) {
        synchronized (activityExecutors) {
            ActivityExecutor executor = activityExecutors.get(activityDef.getAlias());

            if (executor == null) {
                String activityTypeName = activityDef.getParams().getStringOrDefault("type", "diag");
                ActivityType activityType = ActivityTypeFinder.get().get(activityTypeName);
                executor = ActivityExecutorAssembler.getExecutor(activityDef,activityType);

                activityExecutors.put(activityDef.getAlias(), executor);
            }
            return executor;
        }
    }

    public void waitMillis(long remaining) {
        logger.trace("#> waitMillis(" + remaining + ")");
        long endTime = System.currentTimeMillis() + remaining;

        while (remaining > 0L) {
            try {
                Thread.sleep(remaining );
            } catch (InterruptedException spurrious) {
                remaining = endTime - System.currentTimeMillis();
                continue;
            }
            remaining = 0;
        }
    }

    public Set<String> getAliases() {
        return activityExecutors.keySet();
    }

    public List<ActivityDef> getActivityDefs() {
        return activityExecutors.values().stream()
                .map(ActivityExecutor::getActivityDef)
                .collect(Collectors.toList());
    }

    public ActivityDef getActivityDef(String s) {
        return getActivityExecutor(s).getActivityDef();
    }
}
