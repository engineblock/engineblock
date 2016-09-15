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
import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.MetricRegistryService;
import org.mpierce.metrics.reservoir.hdrhistogram.HdrHistogramReservoir;
import org.mpierce.metrics.reservoir.hdrhistogram.HdrHistogramResetOnSnapshotReservoir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

public class ActivityMetrics {

    private final static Logger logger = LoggerFactory.getLogger(ActivityMetrics.class);
    private static MetricRegistry registry;

    private ActivityMetrics() {
    }

    /**
     * Register a named metric for an activity, synchronized on the activity
     *
     * @param activity       The activity that the metric will be for
     * @param name           The full metric name
     * @param metricProvider A function to actually create the metric if needed
     * @return a Metric, or null if the metric for the name was already present
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static Metric register(Activity activity, String name, MetricProvider metricProvider) {
        String fullMetricName = activity.getAlias() + "." + name;
        Metric metric = get().getMetrics().get(fullMetricName);
        if (metric == null) {
            synchronized (activity) {
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

    /**
     * <p>Create a timer associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     *
     * @param activity an associated activity
     * @param name     a simple, descriptive name for the timer
     * @return the timer, perhaps a different one if it has already been registered
     */
    public static Timer timer(Activity activity, String name) {
        Timer registeredTimer = (Timer) register(activity, name, () -> new Timer(new HdrHistogramReservoir()));
        return registeredTimer;
    }

    /**
     * <p>Create a timer with a resetting histogram associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     * <p>A resetting histogram is one that is reset to its initial state every time you take its snapshot.
     * This is useful for gathering histograms for specific spans of time.</p>
     *
     * @param activity an associated activity
     * @param name     a simple, descriptive name for the timer
     * @return the timer, perhaps a different one if it has already been registered
     */
    public static Timer resettingTimer(Activity activity, String name) {
        return (Timer) register(activity, name, () -> new Timer(new HdrHistogramResetOnSnapshotReservoir()));
    }

    /**
     * <p>Create a histogram associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     *
     * @param activity an associated activity
     * @param name     a simple, descriptive name for the histogram
     * @return the histogram, perhaps a different one if it has already been registered
     */
    public static Histogram histogram(Activity activity, String name) {
        return (Histogram) register(activity, name, () -> new Histogram(new HdrHistogramReservoir()));
    }

    /**
     * <p>Create a resetting histogram associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     * <p>A resetting histogram is one that is reset to its initial state every time you take its snapshot.
     * This is useful for gathering histograms for specific spans of time.</p>
     *
     * @param activity an associated activity
     * @param name     a simple, descriptive name for the resetting histogram
     * @return the resetting histogram, perhaps a different one if it has already been registered
     */

    public static Histogram resettingHistogram(Activity activity, String name) {
        return (Histogram) register(activity, name, () -> new NicerHistogram(new HdrHistogramResetOnSnapshotReservoir()));
    }

    /**
     * <p>Create a counter associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     *
     * @param activity an associated activity
     * @param name     a simple, descriptive name for the counter
     * @return the counter, perhaps a different one if it has already been registered
     */
    public static Counter counter(Activity activity, String name) {
        return (Counter) register(activity, name, Counter::new);
    }

    /**
     * <p>Create a meter associated with an activity.</p>
     * <p>This method ensures that if multiple threads attempt to create the same-named metric on a given activity,
     * that only one of them succeeds.</p>
     *
     * @param activity an associated activity
     * @param name     a simple, descriptive name for the meter
     * @return the meter, perhaps a different one if it has already been registered
     */
    public static Meter meter(Activity activity, String name) {
        return (Meter) register(activity, name, Meter::new);
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
