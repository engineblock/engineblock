/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.engineblock.activityapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>A runtime definition for an activity.</p>
 *
 * <p>Instances of ActivityDef hold control values for the execution of a single activity.
 * Each thread of the related activity is initialized with the associated ActivityDef.
 * When the ActivityDef is modified, interested activity threads are notified so that
 * they can dynamically adjust.</p>
 *
 * <p>The canonical values for all parameters are kept internally in the parameter map.
 * Essentially, ActivityDef is just a type-aware wrapper around a thread-safe parameter map,
 * with an atomic change counter which can be used to signal changes to observers.</p>
 */
public class ActivityDef {

    private final static Logger logger = LoggerFactory.getLogger(ActivityDef.class);

    // an alias with which to control the activity while it is running
    private static final String FIELD_ALIAS = "alias";

    // a file or URL containing the activity: statements, generator bindings, ...
    private static final String FIELD_ATYPE = "type";

    // cycles for this activity in either "M" or "N..M" form. "M" form implies "1..M"
    private static final String FIELD_CYCLES = "cycles";

    // initial thread concurrency for this activity
    private static final String FIELD_THREADS = "threads";

    // milliseconds between cycles per thread, for slow tests only
    private static final String FIELD_DELAY = "delay";

    private static final String DEFAULT_ALIAS = "unknown-alias";
    private static final String DEFAULT_ATYPE = "unknown-type";
    private static final String DEFAULT_CYCLES = "1..1";
    private static final int DEFAULT_THREADS = 1;
    private static final int DEFAULT_DELAY = 0;


    // parameter map has its own internal atomic map
    private ParameterMap parameterMap;

    private static String[] field_list = new String[] {
            FIELD_ALIAS, FIELD_ATYPE, FIELD_CYCLES, FIELD_THREADS, FIELD_DELAY
    };
//    private final AtomicInteger atomicThreadTarget = new AtomicInteger(0);

    public ActivityDef(String parameterString) {
        this.parameterMap = ParameterMap.parsePositional(parameterString, field_list);
    }

    public ActivityDef(ParameterMap parameterMap) {
        this.parameterMap = parameterMap;
//        updateAtomicThreadLevel();
    }

    //    private void updateAtomicThreadLevel() {
//        this.atomicThreadTarget.set(getThreads());
//    }

    public String toString() {
        return "ActivityDef:" + parameterMap.toString();
    }

    /**
     * The alias that the associated activity instance is known by.
     * @return the alias
     */
    public String getAlias() {
        return parameterMap.getStringOrDefault("alias",DEFAULT_ALIAS);
    }

    public String getActivityType() {
        return parameterMap.getStringOrDefault("type", DEFAULT_ATYPE);
    }
    /**
     * The first cycle that will be used for execution of this activity, inclusive.
     * @return the long start cycle
     */
    public long getStartCycle() {
        String cycles = parameterMap.getStringOrDefault("cycles",DEFAULT_CYCLES);
        int rangeAt = cycles.indexOf("..");
        if (rangeAt > 0) {
            return Long.valueOf(cycles.substring(0,rangeAt));

        } else {
            return 1L;
        }
    }

    /**
     * The last cycle that will be used for execution of this activity, inclusive.
     * @return the long end cycle
     */
    public long getEndCycle() {
        String cycles = parameterMap.getStringOrDefault("cycles",DEFAULT_CYCLES);
        int rangeAt = cycles.indexOf("..");
        if (rangeAt > 0) {
            return Long.valueOf(cycles.substring(rangeAt+2));
        } else {
            return Long.valueOf(cycles);
        }
    }

    /**
     * The number of threads (AKA slots) that the associated activity should currently be using.
     * @return target thread count
     */
    public int getThreads() {
        return parameterMap.getIntOrDefault(FIELD_THREADS,DEFAULT_THREADS);
    }

    /**
     * The number of milliseconds to delay within each thread for each operation.
     * @return ms delay
     */
    public int getInterCycleDelay() {
        return parameterMap.getIntOrDefault(FIELD_DELAY,DEFAULT_DELAY);
    }

    /**
     * Get the parameter map, which is the backing-store for all data within an ActivityDef.
     * @return the parameter map
     */
    public ParameterMap getParams() {
        return parameterMap;
    }

    public AtomicLong getChangeCounter() {
        return parameterMap.getChangeCounter();
    }

    public void setCycles(String cycles) {
        parameterMap.set(FIELD_CYCLES,cycles);
    }
    public void setStartCycle(long startCycle) {
        parameterMap.set("cycles","" + startCycle + ".." + getEndCycle());
    }
    public void setEndCycle(long endCycle) {
        parameterMap.set(FIELD_CYCLES,"" + getStartCycle() + ".." + endCycle);
    }
    public void setThreads(int threads) {
        parameterMap.set(FIELD_THREADS,threads);
//        updateAtomicThreadLevel();
    }
    public void setDelay(int delay) {
        parameterMap.set(FIELD_DELAY,delay);
    }

    public String getLogName() {
        return toString();
    }

    public static Optional<ActivityDef> parseActivityDefOptionally(String namedActivitySpec) {
        try {
            ActivityDef activityDef = parseActivityDef(namedActivitySpec);
            return Optional.of(activityDef);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static ActivityDef parseActivityDef(String namedActivitySpec) {
        ParameterMap activityParameterMap = ParameterMap.parsePositional(namedActivitySpec, field_list);
        ActivityDef activityDef = new ActivityDef(activityParameterMap);
        logger.debug("parsed activityDef " + namedActivitySpec + " to-> " + activityDef);

        return activityDef;
    }

}
