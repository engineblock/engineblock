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

package io.engineblock.core;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import io.engineblock.activityapi.ActivityMetrics;

import java.io.PrintStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Result {
    private Exception exception;
    private String iolog;

    public Result(String iolog) {
        this.iolog = iolog;
    }

    public Result(Exception e) {
        this.iolog = e.getMessage();
        this.exception = e;
    }

    public void reportTo(PrintStream out) {
        out.println("===================   BEGIN-SCRIPT-LOG   ===================");
        out.print(iolog);
        out.println("===================   END-SCRIPT-LOG   =====================");

        out.println("====================  BEGIN-METRIC-LOG  ====================");
        ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(ActivityMetrics.getMetricRegistry())
                .convertDurationsTo(TimeUnit.MICROSECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .filter(MetricFilter.ALL)
                .outputTo(out)
                .build();
        consoleReporter.report();
        out.println("====================   END-METRIC-LOG   ====================");

    }

    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

    public String getIOLog() {
        return this.iolog;
    }
}
