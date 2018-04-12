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

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.core.ScenarioController;

import javax.script.Bindings;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provide a bindings wrapper around a ScenarioController,
 */
public class ActivityBindings implements Bindings {

    private final ScenarioController scenario;
    private Map<String,Bindings> elementMap = new HashMap<String,Bindings>();

    public ActivityBindings(ScenarioController scenarioController) {
        this.scenario = scenarioController;
    }

    @Override
    public Object put(String name, Object value) {
        throw new RuntimeException("ScenarioBindings do not allow put(...)");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        throw new RuntimeException("ScenarioBindings do not allow putAll(...)");
    }

    @Override
    public void clear() {
        throw new RuntimeException("ScenarioBindings do not allow clear(...)");
    }

    @Override
    public Set<String> keySet() {
        return scenario.getAliases();
    }

    @Override
    public Collection<Object> values() {
        return wrap(scenario.getActivityDefs());
    }

    private Collection<Object> wrap(List<ActivityDef> activityDefs) {
        return activityDefs
                .stream()
                .map(s -> (Bindings) s)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String,Object>> newset = new HashSet<>();
        for (ActivityDef activityDef : scenario.getActivityDefs()) {
            newset.add(new AbstractMap.SimpleImmutableEntry<String, Object>(activityDef.getAlias(),activityDef));
        }
        return newset;
    }

    @Override
    public int size() {
        return scenario.getActivityDefs().size();
    }

    @Override
    public boolean isEmpty() {
        return scenario.getActivityDefs().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return scenario.getAliases().contains(String.valueOf(key));
    }

    @Override
    public boolean containsValue(Object value) {
        throw new RuntimeException("Should this be used?");
    }

    @Override
    public Bindings get(Object key) {
        Bindings activityDef = scenario.getActivityDef(String.valueOf(key)).getParams();
        return activityDef;
    }

    @Override
    public Object remove(Object key) {
        throw new RuntimeException("this is not the advised way to forceStopMotors an activity");
//        scenario.forceStopMotors(String.valueOf(key));
    }
}
