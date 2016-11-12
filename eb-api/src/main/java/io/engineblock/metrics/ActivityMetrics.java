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

package io.engineblock.metrics;

import com.codahale.metrics.*;
import io.engineblock.activityapi.MetricRegistryService;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.util.Unit;
import org.HdrHistogram.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptContext;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ActivityMetrics {

    private final static Logger logger = LoggerFactory.getLogger(ActivityMetrics.class);
    private static MetricRegistry registry;

    public static MetricFilter METRIC_FILTER = (name, metric) -> {
        return true;
    };

    private ActivityMetrics() {
    }

    /**
     * Register a named metric for an activity, synchronized on the activity
     *
     * @param activityDef    The activity def that the metric will be for
     * @param name           The full metric name
     * @param metricProvider A function to actually create the metric if needed
     * @return a Metric, or null if the metric for the name was already present
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static Metric register(ActivityDef activityDef, String name, MetricProvider metricProvider) {
        String fullMetricName = activityDef.getAlias() + "." + name;
        Metric metric = get().getMetrics().get(fullMetricName);
        if (metric == null) {
            synchronized (activityDef) {
                metric = get().getMetrics().get(fullMetricName);
                if (metric == null) {
                    metric = metricProvider.getMetric();
                    return get().register(fullMetricName, metric);
                } else {
                    logger.warn("another thread has created this metric: " + fullMetricName);
                }
            }
        }
        return metric;
    }

    private static Metric register(ScriptContext context, String name, MetricProvider metricProvider) {
        Metric metric = get().getMetrics().get(name);
        if (metric == null) {
            synchronized (context) {
                metric = get().getMetrics().get(name);
                if (metric == null) {
                    metric = metricProvider.getMetric();
                    Metric registered = get().register(name, metric);
                    logger.info("registered scripting metric: " + name);
                    return registered;
                } else {
                    logger.warn("another thread has created this metric: " + name);
                }
            }
        }
        return metric;

    }
    /**
     * <p>Create a timer associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     *
     * @param activityDef an associated activity def
     * @param name     a simple, descriptive name for the timer
     * @return the timer, perhaps a different one if it has already been registered
     */
    public static Timer timer(ActivityDef activityDef, String name) {
        String fullMetricName = activityDef.getAlias() + "." + name;
        Timer registeredTimer = (Timer) register(activityDef, name, () ->
                new NicerTimer(fullMetricName, new DeltaHdrHistogramReservoir(fullMetricName, new Recorder(4))));
        return registeredTimer;
    }

    /**
     * <p>Create a histogram associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     *
     * @param activityDef an associated activity def
     * @param name     a simple, descriptive name for the histogram
     * @return the histogram, perhaps a different one if it has already been registered
     */
    public static Histogram histogram(ActivityDef activityDef, String name) {
        String fullMetricName = activityDef.getAlias() + "." + name;
        return (Histogram) register(activityDef, name, () ->
                new NicerHistogram(fullMetricName, new DeltaHdrHistogramReservoir(fullMetricName, new Recorder(4))));
    }

    /**
     * <p>Create a counter associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     *
     * @param activityDef an associated activity def
     * @param name     a simple, descriptive name for the counter
     * @return the counter, perhaps a different one if it has already been registered
     */
    public static Counter counter(ActivityDef activityDef, String name) {
        return (Counter) register(activityDef, name, Counter::new);
    }

    /**
     * <p>Create a meter associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     *
     * @param activityDef an associated activity def
     * @param name     a simple, descriptive name for the meter
     * @return the meter, perhaps a different one if it has already been registered
     */
    public static Meter meter(ActivityDef activityDef, String name) {
        return (Meter) register(activityDef, name, Meter::new);
    }

    private static MetricRegistry get() {
        if (registry != null) {
            return registry;
        }
        synchronized (ActivityMetrics.class) {
            if (registry == null) {
                registry = lookupRegistry();
            }
        }
        return registry;
    }

    public static Gauge<?> gauge(ActivityDef activityDef, String name, Gauge<?> gauge) {
        return (Gauge<?>) register(activityDef, name, () -> gauge);
    }

    public static Gauge<?> gauge(ScriptContext scriptContext, String name, Gauge<?> gauge) {
        return (Gauge<?>) register(scriptContext, name, () -> gauge);
    }


    private static MetricRegistry lookupRegistry() {
        ServiceLoader<MetricRegistryService> metricRegistryServices =
                ServiceLoader.load(MetricRegistryService.class);
        List<MetricRegistryService> mrss = new ArrayList<>();
        metricRegistryServices.iterator().forEachRemaining(mrss::add);
        if (mrss.size() == 1) {
            return mrss.get(0).getMetricRegistry();
        }

        String errorMsg = "found " + mrss.size() + " MetricRegistryServices instead of 1";
        logger.error(errorMsg);
        throw new RuntimeException(errorMsg);
    }


    public static MetricRegistry getMetricRegistry() {
        return get();
    }

    /**
     * Add a histogram logger to matching metrics in this JVM instance.
     * @param sessionName The name for the session to be annotated in the histogram log
     * @param pattern A regular expression pattern to filter out metric names for logging
     * @param filename A file to log the histogram data in
     * @param interval How many seconds to wait between writing each interval histogram
     */
    public static void addHistoLogger(String sessionName, String pattern, String filename, String interval) {
        if (filename.contains("_SESSION_")) {
            filename = filename.replace("_SESSION_",sessionName);
        }
        Pattern compiledPattern = Pattern.compile(pattern);
        File logfile = new File(filename);
        long intervalMillis = Unit.msFor(interval);

        HistoIntervalLogger histoIntervalLogger =
                new HistoIntervalLogger(sessionName, logfile, compiledPattern, intervalMillis);
        logger.debug("attaching " + histoIntervalLogger + " to the metrics registry.");
        get().addListener(histoIntervalLogger);
    }

    private static interface MetricProvider {
        Metric getMetric();
    }

    public static void reportTo(PrintStream out) {
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

}
