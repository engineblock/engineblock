package com.metawiring.tools.activityapi;

import java.util.concurrent.atomic.AtomicLong;

public interface ActivityDef {


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
