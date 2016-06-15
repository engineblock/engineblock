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

package io.engineblock.script;

import javax.script.Bindings;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** A convenience class to make read-only Bindings easier to write.
 * This will not make access to the context efficient, but it will make it easier to do correctly.
 * More advanced implementations are recommended when the cost of indirecting through a map on access is too high.
 *
 * @param <T> context object type
 */
public abstract class ReadOnlyBindings<T> implements Bindings {

    protected T contextObject;
//    protected Map<String,Object> map = new HashMap<String,Object>();

    public ReadOnlyBindings(T contextObject) {
        this.contextObject = contextObject;
//        this.map = getMap(contextObject);
    }

    @Override
    public Object put(String name, Object value) {
        throw new ReadOnlyBindingsException(this, "put");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        throw new ReadOnlyBindingsException(this,"putAll");
    }

    @Override
    public void clear() {
        throw new ReadOnlyBindingsException(this,"clear");
    }

    @Override
    public Set<String> keySet() {
        return getMap().keySet();
    }

    @Override
    public Collection<Object> values() {
        return getMap().values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return getMap().entrySet();
    };

    @Override
    public int size() {
        return getMap().size();
    }

    @Override
    public boolean isEmpty() {
        return getMap().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return getMap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return getMap().containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return getMap().get(key);
    }

    @Override
    public Object remove(Object key) {
        throw new ReadOnlyBindingsException(this, "remove");
    }

    private Map<String,Object> getMap() {
        return getMap(contextObject);
    }

    protected abstract Map<String,Object> getMap(T contextObject);

}
