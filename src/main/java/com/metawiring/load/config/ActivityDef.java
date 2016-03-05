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

package com.metawiring.load.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
 * with an atomic change counter which can be used to signal changes to observers./p>
 */
public class ActivityDef {

    private final static Logger logger = LoggerFactory.getLogger(ActivityDef.class);

    // an alias with which to control the activity while it is running
    private static final String FIELD_ALIAS = "alias";

    // a file or URL containing the activity: statements, generator bindings, ...
    private static final String FIELD_SOURCE = "source";

    // cycles for this activity in either "M" or "N..M" form. "M" form implies "1..M"
    private static final String FIELD_CYCLES = "cycles";

    // initial thread concurrency for this activity
    private static final String FIELD_THREADS = "threads";

    // number of ops to keep in-flight
    private static final String FIELD_ASYNC = "async";

    // milliseconds between cycles per thread, for slow tests only
    private static final String FIELD_DELAY = "delay";

    private static final String DEFAULT_ALIAS = "unknown-alias";
    private static final String DEFAULT_SOURCE = "unknown-source";
    private static final String DEFAULT_CYCLES = "1..1";
    private static final int DEFAULT_THREADS = 1;
    private static final int DEFAULT_ASYNC = 1;
    private static final int DEFAULT_DELAY = 0;


    // parameter map has its own internal atomic map
    private ParameterMap parameterMap;

    private static String[] field_list = new String[] {
            FIELD_ALIAS, FIELD_SOURCE, FIELD_CYCLES, FIELD_THREADS, FIELD_ASYNC, FIELD_DELAY
    };
//    private final AtomicInteger atomicThreadTarget = new AtomicInteger(0);

    public ActivityDef(String parameterString) {
        this.parameterMap = ParameterMap.parsePositional(parameterString, field_list);
    }
    protected ActivityDef(ParameterMap parameterMap) {
        this.parameterMap = parameterMap;
//        updateAtomicThreadLevel();
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
        ParameterMap activityParameterMap = ParameterMap.parsePositional(namedActivitySpec,field_list);
        ActivityDef activityDef = new ActivityDef(activityParameterMap);
        return activityDef;
    }

//    private void updateAtomicThreadLevel() {
//        this.atomicThreadTarget.set(getThreads());
//    }

    public String toString() {
        return "ActivityDef:" + parameterMap.toString();
    }

    public String getAlias() {
        return parameterMap.getStringOrDefault("alias",DEFAULT_ALIAS);
    }

    public long getStartCycle() {
        String cycles = parameterMap.getStringOrDefault("cycles",DEFAULT_CYCLES);
        int rangeAt = cycles.indexOf("..");
        if (rangeAt > 0) {
            return Long.valueOf(cycles.substring(0,rangeAt));

        } else {
            return 1L;
        }
    }

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
     * Returns the greater of threads or maxAsync. The reason for this is that maxAsync less than threads will starve
     * threads of async grants, since the async is apportioned to threads in an activity.
     *
     * @return maxAsync, or threads if threads is greater
     */
    public int getMaxAsync() {
        int async = parameterMap.getIntOrDefault(FIELD_ASYNC,DEFAULT_ASYNC);
        int threads = parameterMap.getIntOrDefault(FIELD_THREADS,DEFAULT_THREADS);
        return (threads > async) ? threads : async;
    }

    public int getThreads() {
        return parameterMap.getIntOrDefault(FIELD_THREADS,DEFAULT_THREADS);
    }

    public int getInterCycleDelay() {
        return parameterMap.getIntOrDefault(FIELD_DELAY,DEFAULT_DELAY);
    }

    public ParameterMap getParams() {
        return parameterMap;
    }

    public String getSource() {
        return parameterMap.getStringOrDefault("source",DEFAULT_SOURCE);
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
    public void setAsync(int async) {
        parameterMap.set(FIELD_ASYNC,async);
    }
    public void setDelay(int delay) {
        parameterMap.set(FIELD_DELAY,delay);
    }

    public String getLogName() {
        return toString();
    }

}
