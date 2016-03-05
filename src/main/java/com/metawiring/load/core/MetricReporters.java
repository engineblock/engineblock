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

package com.metawiring.load.core;

import com.codahale.metrics.*;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MetricReporters implements IShutdown {
    private final static Logger logger = LoggerFactory.getLogger(MetricReporters.class);
    private static MetricReporters instance = new MetricReporters();

    private List<PrefixedRegistry> metricRegistries = new ArrayList<>();
    private List<ScheduledReporter> scheduledReporters = new ArrayList<>();

    private MetricReporters() {
        ShutdownManager.register(this);
    }

    public static MetricReporters getInstance() {
        return instance;
    }

    public MetricReporters addRegistry(String registryPrefix, MetricRegistry metricsRegistry) {
        this.metricRegistries.add(new PrefixedRegistry(registryPrefix, metricsRegistry));
        return this;
    }

    public MetricReporters addGraphite(String host, int graphitePort, String prefix) {

        if (metricRegistries.isEmpty()) {
            throw new RuntimeException("There are no metric registries.");
        }

        for (PrefixedRegistry prefixedRegistry : metricRegistries) {

            Graphite graphite = new Graphite(new InetSocketAddress(host, graphitePort));
            GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(prefixedRegistry.metricRegistry)
                    .prefixedWith(prefixedRegistry.prefix != null ? (!prefixedRegistry.prefix.isEmpty() ? prefix + "." + prefixedRegistry.prefix : prefix) : prefix)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .build(graphite);

            scheduledReporters.add(graphiteReporter);
        }
        return this;
    }

//    public MetricReporters addConsole(MetricRegistry registry) {
//        ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(registry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .filter(MetricFilter.ALL)
//                .build();
//
//        scheduledReporters.add(consoleReporter);
//        return this;
//    }

    public MetricReporters addLogger() {

        if (metricRegistries.isEmpty()) {
            throw new RuntimeException("There are no metric registries.");
        }

        for (PrefixedRegistry prefixedRegistry : metricRegistries) {

            Slf4jReporter loggerReporter = Slf4jReporter.forRegistry(prefixedRegistry.metricRegistry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .outputTo(logger)
                    .build();
            scheduledReporters.add(loggerReporter);
        }
        return this;
    }

    public MetricReporters start() {
        for (ScheduledReporter scheduledReporter : scheduledReporters) {
            logger.info("starting reporter: " + scheduledReporter);
            if (scheduledReporter instanceof ConsoleReporter) {
                scheduledReporter.start(10, TimeUnit.SECONDS);
            } else {
                scheduledReporter.start(1, TimeUnit.MINUTES);
            }
        }
        return this;
    }

    public MetricReporters stop() {
        for (ScheduledReporter scheduledReporter : scheduledReporters) {
            logger.info("stopping reporter: " + scheduledReporter);
            scheduledReporter.stop();
        }
        return this;
    }


    public MetricReporters report() {
        for (ScheduledReporter scheduledReporter : scheduledReporters) {
            logger.info("flushing reporter data: " + scheduledReporter);
            scheduledReporter.report();
        }
        return this;
    }

    public void shutdown() {
        for (ScheduledReporter reporter : scheduledReporters) {
            reporter.report();
            reporter.stop();
        }
    }

    private class PrefixedRegistry {
        public String prefix;
        public MetricRegistry metricRegistry;

        public PrefixedRegistry(String prefix, MetricRegistry metricRegistry) {
            this.prefix = prefix;
            this.metricRegistry = metricRegistry;
        }
    }
}
