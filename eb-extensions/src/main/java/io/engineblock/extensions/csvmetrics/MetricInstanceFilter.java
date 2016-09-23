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
package io.engineblock.extensions.csvmetrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import java.util.ArrayList;
import java.util.List;

public class MetricInstanceFilter implements MetricFilter {

    private List<Metric> included = new ArrayList<Metric>();

    public MetricInstanceFilter add(Metric metric) {
        this.included.add(metric);
        return this;
    }

    @Override
    public boolean matches(String name, Metric metric) {
        return included.isEmpty() || included.stream().anyMatch(m -> m==metric);
    }
}
