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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.metawiring.load.config.TestClientConfig;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

public class MetricsContext {

    private static MetricsContext instance;

    private MetricReporters metricReporters = MetricReporters.getInstance();
    private MetricRegistry metrics = new MetricRegistry();

    public static MetricsContext getInstance() {
        synchronized (MetricsContext.class) {
            if (instance==null) {
                instance = new MetricsContext();
            }
        }
        return instance;
    }

    public MetricReporters getReporters() {
        return metricReporters;
    }

    public MetricRegistry getMetrics() {
        return metrics;
    }

    /**
     * Convenience method to unclutter code. This will be used everywhere.
     * @return The default metric context.
     */
    public static MetricRegistry metrics() {
        return getInstance().getMetrics();
    }
}
