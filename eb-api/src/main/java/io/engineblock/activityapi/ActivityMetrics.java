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

package io.engineblock.activityapi;

import com.codahale.metrics.*;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ActivityMetrics {

    private final static Logger logger = LoggerFactory.getLogger(ActivityMetrics.class);
    private static MetricRegistry registry;

    private ActivityMetrics() {
    }

    public static Timer timer(ActivityDef activityDef, String name) {
        Timer timer = get().timer(activityDef.getAlias() + "." + name);
        return timer;
    }

    public static Histogram histogram(ActivityDef activityDef, String name) {
        Histogram histogram = get().histogram(activityDef.getAlias() + "." + name);
        return histogram;
    }

    public static Counter counter(ActivityDef activityDef, String name) {
        Counter counter = get().counter(activityDef.getAlias() + "." + name);
        return counter;
    }

    public static Meter meter(ActivityDef activityDef, String name) {
        Meter meter = get().meter(activityDef.getAlias() + "." + name);
        return meter;
    }

// TODO: https://github.com/engineblock/engineblock/issues/56
//    public static <T> Gauge<T> gauge(ActivityDef activityDef, String name, Gauge<T> gauge) {
//        gauge = get().register(activityDef.getAlias() + "." + name, gauge); // use or replace gauge
//        return gauge;
//    }

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
}
