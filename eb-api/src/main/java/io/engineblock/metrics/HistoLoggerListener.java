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

import java.util.HashMap;
import java.util.Map;

/**
 * A silly class that does nothing but allow cleaner code elsewhere,
 * because MetricRegistryListener, that's why.
 */
public abstract class HistoLoggerListener implements MetricRegistryListener {

    private Map<String,HistoLogger> capables = new HashMap<>();

    public abstract void onHistogramLogCapableAdded(String name, HistoLogger capable);
    public abstract void onHistogramLogCapableRemoved(String name, HistoLogger capable);

    @Override
    public void onHistogramAdded(String name, Histogram metric) {
        if (metric instanceof HistoLogger) {
            HistoLogger capable = (HistoLogger)metric;
            capables.put(name,capable);
            onHistogramLogCapableAdded(name, capable);
        }
    }

    @Override
    public void onHistogramRemoved(String name) {
        HistoLogger removed = capables.remove(name);
        if (removed!=null) {
            onHistogramLogCapableRemoved(name, removed);
        }
    }

    @Override
    public void onTimerAdded(String name, Timer metric) {
        if (metric instanceof HistoLogger) {
            HistoLogger capable = (HistoLogger)metric;
            capables.put(name,capable);
            onHistogramLogCapableAdded(name, capable);
        }
    }

    @Override
    public void onTimerRemoved(String name) {
        HistoLogger removed = capables.remove(name);
        if (removed!=null) {
            onHistogramLogCapableRemoved(name, removed);
        }
    }

    @Override
    public void onGaugeAdded(String name, Gauge<?> metric) {
        if (metric instanceof HistoLogger) {
            HistoLogger capable = (HistoLogger)metric;
            capables.put(name,capable);
            onHistogramLogCapableAdded(name, capable);
        }
    }

    @Override
    public void onGaugeRemoved(String name) {
        HistoLogger removed = capables.remove(name);
        if (removed!=null) {
            onHistogramLogCapableRemoved(name, removed);
        }
    }

    @Override
    public void onCounterAdded(String name, Counter metric) {
        if (metric instanceof HistoLogger) {
            HistoLogger capable = (HistoLogger)metric;
            capables.put(name,capable);
            onHistogramLogCapableAdded(name, capable);
        }
    }

    @Override
    public void onCounterRemoved(String name) {
        HistoLogger removed = capables.remove(name);
        if (removed!=null) {
            onHistogramLogCapableRemoved(name, removed);
        }
    }

    @Override
    public void onMeterAdded(String name, Meter metric) {
        if (metric instanceof HistoLogger) {
            HistoLogger capable = (HistoLogger)metric;
            capables.put(name,capable);
            onHistogramLogCapableAdded(name, capable);
        }
    }

    @Override
    public void onMeterRemoved(String name) {
        HistoLogger removed = capables.remove(name);
        if (removed!=null) {
            onHistogramLogCapableRemoved(name, removed);
        }
    }
}
