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
package io.engineblock.core;

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityapi.ActivityType;
import io.engineblock.activityimpl.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A ScenarioController provides a way to start Activities, modify them while running, and forceStopMotors, pause or restart them.
 */
public class ScenarioController {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioController.class);

    private final Map<String, ActivityExecutor> activityExecutors = new HashMap<>();

    /**
     * Start an activity, given the activity definition for it. The activity will be known in the scenario
     * by the alias parameter.
     *
     * @param activityDef string in alias=value1;type=value2;... format
     */
    public synchronized void start(ActivityDef activityDef) {
        getActivityExecutor(activityDef).startActivity();
    }

    /**
     * Start an activity, given a map which holds the activity definition for it. The activity will be known in
     * the scenario by the alias parameter.
     *
     * @param activityDefMap A map containing the activity definition
     */
    public synchronized void start(Map<String, String> activityDefMap) {
        ActivityDef ad = new ActivityDef(new ParameterMap(activityDefMap));
        start(ad);
    }

    public synchronized void run(int timeout, Map<String, String> activityDefMap) {
        ActivityDef ad = new ActivityDef(new ParameterMap(activityDefMap));
        run(timeout, ad);
    }

    public synchronized void run(int timeout, ActivityDef activityDef) {
        ActivityExecutor activityExecutor = getActivityExecutor(activityDef);
        activityExecutor.startActivity();
        activityExecutor.awaitCompletion(timeout);
    }

    public synchronized void run(int timeout, String activityDefString) {
        ActivityDef activityDef = ActivityDef.parseActivityDef(activityDefString);
        run(timeout,activityDef);
    }

    /**
     * Start an activity, given the name by which it is known already in the scenario. This is useful if you have
     * stopped an activity and want to start it again.
     *
     * @param alias the alias of an activity that is already known to the scenario
     */
    public synchronized void start(String alias) {
        start(ActivityDef.parseActivityDef(alias));
    }

    public boolean isRunningActivity(String alias) {
        return isRunningActivity(ActivityDef.parseActivityDef(alias));
    }

    public boolean isRunningActivity(ActivityDef activityDef) {
        return getActivityExecutor(activityDef).isRunning();
    }

    public boolean isRunningActivity(Map<String, String> activityDefMap) {
        ActivityDef ad = new ActivityDef(new ParameterMap(activityDefMap));
        return isRunningActivity(ad);
    }

    /**
     * <p>Stop an activity, given an activity def. The only part of the activity def that is important is the
     * alias parameter. This method retains the activity def signature to provide convenience for scripting.</p>
     * <p>For example, sc.stop("alias=foo")</p>
     *
     * @param activityDef An activity def, including at least the alias parameter.
     */
    public synchronized void stop(ActivityDef activityDef) {
        getActivityExecutor(activityDef).stopActivity();
    }

    /**
     * <p>Stop an activity, given an activity def map. The only part of the map that is important is the
     * alias parameter. This method retains the map signature to provide convenience for scripting.</p>
     *
     * @param activityDefMap A map, containing at least the alias parameter
     */
    public synchronized void stop(Map<String, String> activityDefMap) {
        ActivityDef ad = new ActivityDef(new ParameterMap(activityDefMap));
        stop(ad);
    }

    /**
     * Stop an activity, given the name by which it is known already in the scenario. This causes the
     * activity to stop all threads, but keeps the thread objects handy for starting again. This can be useful
     * for certain testing scenarios in which you want to stop some workloads and start others based on other conditions.
     *
     * @param alias The name of the activity that is already known to the scenario
     */
    public synchronized void stop(String alias) {
        stop(ActivityDef.parseActivityDef(alias));
    }

    /**
     * Modify one of the parameters in a defined activity. Any observing activity components will be notified of the
     * changes made to activity parameters.
     *
     * @param alias The name of an activity that is already known to the scenario.
     * @param param The parameter name
     * @param value a new parameter value
     */
    public synchronized void modify(String alias, String param, String value) {
        if (param.equals("alias")) {
            throw new InvalidParameterException("It is not allowed to change the name of an existing activity.");
        }
        ActivityExecutor activityExecutor = getActivityExecutor(alias);
        ParameterMap params = activityExecutor.getActivityDef().getParams();
        params.set(param, value);
    }

    /**
     * Apply any parameter changes to a defined activity, or start a new one.
     * This method is syntactical sugar for scripting. Each of the parameters in the map
     * is checked against existing values, and per-field modifications
     * are applied one at a time, only if the values have changed.
     *
     * @param appliedParams Map of new values.
     */
    public synchronized void apply(Map<String, String> appliedParams) {
        String alias = appliedParams.get("alias");

        if (alias == null) {
            throw new RuntimeException("alias must be provided");
        }

        ActivityExecutor executor = activityExecutors.get(alias);

        if (executor == null) {
            logger.info("started scenario from apply:" + alias);
            start(appliedParams);
            return;
        }

        ParameterMap previousMap = executor.getActivityDef().getParams();

        for (String paramName : appliedParams.keySet()) {
            String appliedVal = appliedParams.get(paramName);
            Optional<String> prevVal = previousMap.getOptionalString(paramName);

            if (!prevVal.isPresent() || !prevVal.get().equals(appliedVal)) {
                logger.info("applying new value to activity '" + alias + "': '" + prevVal.get() + "' -> '" + appliedVal + "'");
                previousMap.set(paramName, appliedVal);
            }
        }
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

    private ActivityExecutor getActivityExecutor(ActivityDef activityDef) {
        synchronized (activityExecutors) {
            ActivityExecutor executor = activityExecutors.get(activityDef.getAlias());

            if (executor == null) {
                String activityTypeName = activityDef.getParams().getStringOrDefault("type", "diag");
                ActivityType activityType = ActivityTypeFinder.instance().getOrThrow(activityTypeName);
                executor = ActivityExecutorAssembler.getExecutor(activityDef, activityType);

                activityExecutors.put(activityDef.getAlias(), executor);
            }
            return executor;
        }
    }

    /**
     * Wait for a bit. This is not the best approach, and will be replace with a different system in the future.
     *
     * @param waitMillis time to wait, in milliseconds
     */
    public void waitMillis(long waitMillis) {
        logger.trace("#> waitMillis(" + waitMillis + ")");
        long endTime = System.currentTimeMillis() + waitMillis;

        while (waitMillis > 0L) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException spurrious) {
                waitMillis = endTime - System.currentTimeMillis();
                continue;
            }
            waitMillis = 0;
        }
    }

    /**
     * Return all the names of the activites that are known to this scenario.
     *
     * @return set of activity names
     */
    public Set<String> getAliases() {
        return activityExecutors.keySet();
    }

    /**
     * Return all the activity definitions that are known to this scenario.
     *
     * @return list of activity defs
     */
    public List<ActivityDef> getActivityDefs() {
        return activityExecutors.values().stream()
                .map(ActivityExecutor::getActivityDef)
                .collect(Collectors.toList());
    }

    /**
     * Get the named activity def, if it is known to this scenario.
     *
     * @param alias The name by which the activity is known to this scenario.
     * @return an ActivityDef instance
     * @throws RuntimeException if the alias is not known to the scenario
     */
    public ActivityDef getActivityDef(String alias) {
        return getActivityExecutor(alias).getActivityDef();
    }

    /**
     * Force the scenario to stop running. Stop all activity threads, and after waitTimeMillis, force stop
     * all activity threads. An activity will stop its threads cooperatively in this time as long as the
     * internal cycles complete before the timer expires.
     *
     * @param waitTimeMillis grace period during which an activity may cooperatively shut down
     */
    public void forceStopScenario(int waitTimeMillis) {
        logger.warn("Scenario force stopped.");
        activityExecutors.values().forEach(a -> a.forceStopExecutor(waitTimeMillis));
    }

    /**
     * Await completion of all running activities, but do not force shutdownActivity. This method is meant to provide
     * the blocking point for calling logic. It waits.
     *
     * @param waitTimeMillis The time to wait, usually set very high
     * @return true, if all activities completed before the timer expired, false otherwise
     */
    public boolean awaitCompletion(int waitTimeMillis) {
        boolean completed = false;
        for (ActivityExecutor executor : activityExecutors.values()) {
            if (!executor.awaitCompletion(waitTimeMillis))
                return false;
        }
        return true;
    }

    /**
     * @return an unmodifyable String to executor map of all activities known to this scenario
     */
    public Map<String, ActivityExecutor> getActivityMap() {
        return Collections.unmodifiableMap(activityExecutors);
    }
}
