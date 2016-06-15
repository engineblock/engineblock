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
import com.codahale.metrics.MetricRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Provide a Bindings layer for metrics.</p>
 *
 * <p>This is not straight-forward because the dotted naming scheme used for
 * metrics is not always used for layering. For example, a metric named "my.basic.metric"
 * may exist only at one level in the metrics data, so looking for it under "my" or
 * "my.basic" will not work.</p>
 *
 * <p>Since the dotted naming scheme will be presumed by most users to represent tree
 * traversal, this binding layer will create the structure to support such navigation</p>
 */
public class MetricsBindings extends ReadOnlyBindings<MetricRegistry> {

    public MetricsBindings(MetricRegistry contextObject) {
        super(contextObject);
    }

    @Override
    protected Map<String, Object> getMap(MetricRegistry contextObject) {
        Map<String,Object> map = new HashMap<String,Object>();

        for (Entry<String, Metric> entry : contextObject.getMetrics().entrySet()) {
            map.put(entry.getKey(),entry.getValue());
        }

        return map;
    }

    /**
     * For debugging purposes - remove this
     * @param key key
     * @return value
     */
    @Override
    public Object get(Object key) {
        Object o = super.get(key);
        return o;
    }
}
