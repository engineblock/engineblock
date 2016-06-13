/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package io.engineblock.script;

import com.codahale.metrics.Metric;
import io.engineblock.core.MetricsContext;

import javax.script.Bindings;
import java.util.*;

public class MetricsBindings implements Bindings {

    private final MetricsContext context;

    public MetricsBindings(MetricsContext metricsContext) {
        this.context = metricsContext;
    }

    @Override
    public Object put(String name, Object value) {
        throw new RuntimeException("Metrics are read only.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        throw new RuntimeException("Metrics are read only.");
    }

    @Override
    public void clear() {
        throw new RuntimeException("Metrics are read only.");
    }

    @Override
    public Set<String> keySet() {
        return context.getMetrics().getNames();
    }

    @Override
    public Collection<Object> values() {
        Collection<Object> values = new ArrayList<Object>();
        values.addAll(
            context.getMetrics().getMetrics().values()
        );
        return values;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String,Object>> newset = new HashSet<>();
        for (Entry<String, Metric> entry : context.getMetrics().getMetrics().entrySet()) {
            newset.add(new AbstractMap.SimpleImmutableEntry<String, Object>(entry));
        }
        return newset;
    }

    @Override
    public int size() {
        return context.getMetrics().getMetrics().size();
    }

    @Override
    public boolean isEmpty() {
        return context.getMetrics().getMetrics().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return context.getMetrics().getNames().contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return context.getMetrics().getMetrics().values().contains(value);
    }

    @Override
    public Object get(Object key) {
        return context.getMetrics().getMetrics().get(key);
    }

    @Override
    public Object remove(Object key) {
        throw new RuntimeException("Metrics are read only.");
    }
}
