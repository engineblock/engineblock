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

package com.metawiring.load.activitytypes;

import com.metawiring.load.activityapi.Activity;
import com.metawiring.load.core.MetricsContext;
import com.metawiring.load.core.OldExecutionContext;
import com.metawiring.load.core.IndexedThreadFactory;
import com.metawiring.load.generator.GeneratorBindingList;
import com.metawiring.load.generator.GeneratorInstanceSource;

import static com.codahale.metrics.MetricRegistry.name;

public abstract class BaseActivity implements Activity {
    protected OldExecutionContext context;
    protected String activityName;
    protected boolean diagnoseExceptions = true;
    protected GeneratorInstanceSource generatorInstanceSource;

    @Override
    public void cleanup() { }

    protected String getThreadMetricName(String metricIdentifier) {
        String threadName = ((IndexedThreadFactory.IndexedThread) Thread.currentThread()).getMetricName();
        int threadIndex = ((IndexedThreadFactory.IndexedThread) Thread.currentThread()).getThreadIndex();
        String threadMetricName = name(
                getClass().getSimpleName(),
                threadName,
                String.valueOf(threadIndex),
                metricIdentifier);
        return threadMetricName;
    }
    protected void setThreadMetricBasename(String threadMetricName) {
        ((IndexedThreadFactory.IndexedThread) Thread.currentThread()).setMetricName(threadMetricName);
    }

    protected void instrumentException(Exception e) {
        String exceptionType = e.getClass().getSimpleName();
        MetricsContext.metrics().meter(name(getClass().getSimpleName(), "exceptions", exceptionType)).mark();
        if (diagnoseExceptions) {
            throw new RuntimeException(e);
        }
    }

    protected GeneratorBindingList createGeneratorBindings() {
        return new GeneratorBindingList(generatorInstanceSource);
    }

}

