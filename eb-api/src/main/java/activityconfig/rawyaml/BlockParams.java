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

package activityconfig.rawyaml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * StmtProps capture the configuration properties that can be used to control
 * how statements are used. They can be set at multipe levels of configuration.
 * <p>
 * StmtProps include:
 * <ul>
 * <li>An optional name - for identifying statements in metrics and logs</li>
 * <li>An optional set of tags - for specifying which statements to include</li>
 * <li>An optional set of bindings - for associating data with a statement</li>
 * <li>An optional set of config - for configuring the individual statements, in a driver-specific way</li>
 * </ul>
 * <p>
 * Since every layer of a configuration file (statement yaml format) can have the
 * fields above, they will be layered as appropriate for each element.
 * Generally speaking, names are concatenated with hyphens to yield more specific names,
 * while bindings, config and tags are layered, with same-named bindings overriding at
 * lower levels.
 * See {@link RawStmtsBlock} and {@link RawStmtsDoc} for more details.
 * </p>
 * <p>
 * The name represents a symbolic name for the statements in the list. Each statement will
 * be given a specific name by enumeration, so the first statement in the below example will
 * be named "testname-1", the second "testname-2" and so on.
 * <p>
 * The bindings will be used to apply properties and data to each of the statements individually.
 * For bindings that contain data,
 * <p>
 * Example yaml:
 * <pre>
 * name: testname
 * tags:
 *   group: updates
 * config:
 *   timeout: 12345ms
 * bindings:
 *   field1: Static(5)
 *   field2: ToDate()
 * </pre>
 */
public class BlockParams extends Tags {

    private String name = "";
    private Map<String, String> bindings = new LinkedHashMap<>();
    private Map<String, String> params = new LinkedHashMap<>();

    public BlockParams() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public void setBindings(Map<String, String> bindings) {
        this.bindings.clear();
        this.bindings.putAll(bindings);
    }

    public Map<String, String> getParams() {
        return Collections.unmodifiableMap(params);
    }

    public void setParams(Map<String, String> config) {
        this.params.clear();
        this.params.putAll(config);
    }

    public void applyBlockParams(BlockParams other) {
        setName(other.getName());
        setBindings(other.getBindings());
        setTags(other.getTags());
        setParams(other.getParams());
    }
}
