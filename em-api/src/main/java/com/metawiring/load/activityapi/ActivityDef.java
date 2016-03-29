package com.metawiring.load.activityapi;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public interface ActivityDef {
    static Optional<ActivityDef> parseActivityDefOptionally(String namedActivitySpec) {
        try {
            ActivityDef activityDef = parseActivityDef(namedActivitySpec);
            return Optional.of(activityDef);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static ActivityDef parseActivityDef(String namedActivitySpec) {
        ParameterMap activityParameterMap = ParameterMap.parsePositional(namedActivitySpec, IActivityDef.field_list);
        ActivityDef activityDef = new ActivityDef(activityParameterMap);
        return activityDef;
    }

    String getAlias();

    long getStartCycle();

    long getEndCycle();

    int getThreads();

    ParameterMap getParams();

    AtomicLong getChangeCounter();

    void setCycles(String cycles);

    void setStartCycle(long startCycle);

    void setEndCycle(long endCycle);

    void setThreads(int threads);

    void setAsync(int async);

    void setDelay(int delay);

    String getLogName();
}
