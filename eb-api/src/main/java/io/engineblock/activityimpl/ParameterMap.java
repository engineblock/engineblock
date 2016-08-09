/*
*   Copyright 2015 jshook
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

package io.engineblock.activityimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>A concurrently accessible parameter map which holds both keys and values as strings.
 * An atomic change counter tracks updates, to allow interested consumers to determine
 * when to re-read values across threads. The basic format is
 * &lt;paramname&gt;=&lt;paramvalue&gt;;...</p>
 *
 * <p>To create a parameter map, use one of the static parse... methods.</p>
 *
 * <p>No non-String types are used internally. Everything is encoded as a String, even though the
 * generic type is parameterized for Bindings support.</p>
 */
public class ParameterMap extends ConcurrentHashMap<String,Object> implements Bindings {
    private final static Logger logger = LoggerFactory.getLogger(ParameterMap.class);


//    private final ConcurrentHashMap<String, String> paramMap = new ConcurrentHashMap<>(10);
    private final AtomicLong changeCounter = new AtomicLong(0L);
    private final LinkedList<Listener> listeners = new LinkedList<>();

    public ParameterMap(Map<String, String> valueMap) {
        logger.trace("new parameter map:" + valueMap.toString());
        super.putAll(valueMap);
    }

    public long getLongOrDefault(String paramName, long defaultLongValue) {
        Optional<String> l = Optional.ofNullable(super.get(paramName)).map(String::valueOf);
        return l.map(Long::valueOf).orElse(defaultLongValue);
    }

    public double getDoubleOrDefault(String paramName, double defaultDoubleValue) {
        Optional<String> d = Optional.ofNullable(super.get(paramName)).map(String::valueOf);
        return d.map(Double::valueOf).orElse(defaultDoubleValue);
    }

    public String getStringOrDefault(String paramName, String defaultStringValue) {
        Optional<String> s = Optional.ofNullable(super.get(paramName)).map(String::valueOf);
        return s.orElse(defaultStringValue);
    }

    public Optional<String> getOptionalString(String paramName) {
        return Optional.ofNullable(super.get(paramName)).map(String::valueOf);
    }

    public Optional<Long> getOptionalLong(String paramName) {
        return Optional.ofNullable(super.get(paramName)).map(String::valueOf).map(Long::valueOf);
    }

    public Optional<Double> getOptionalDouble(String paramName) {
        return Optional.ofNullable(super.get(paramName)).map(String::valueOf).map(Double::valueOf);
    }

    public Optional<Boolean> getOptionalBoolean(String paramName) {
        return Optional.ofNullable(super.get(paramName)).map(String::valueOf).map(Boolean::valueOf);
    }

    public int getIntOrDefault(String paramName, int defaultIntValue) {
        Optional<String> i = Optional.ofNullable(super.get(paramName)).map(String::valueOf);
        return i.map(Integer::valueOf).orElse(defaultIntValue);
    }

    public boolean getBoolOrDefault(String paramName, boolean defaultBoolValue) {
        Optional<String> b = Optional.ofNullable(super.get(paramName)).map(String::valueOf);
        return b.map(Boolean::valueOf).orElse(defaultBoolValue);
    }


    public Long takeLongOrDefault(String paramName, Long defaultLongValue) {
        Optional<String> l = Optional.ofNullable(super.remove(paramName)).map(String::valueOf);
        Long lval = l.map(Long::valueOf).orElse(defaultLongValue);
        markMutation();
        return lval;
    }

    public Double takeDoubleOrDefault(String paramName, double defaultDoubleValue) {
        Optional<String> d = Optional.ofNullable(super.remove(paramName)).map(String::valueOf);
        Double dval = d.map(Double::valueOf).orElse(defaultDoubleValue);
        markMutation();
        return dval;
    }

    public String takeStringOrDefault(String paramName, String defaultStringValue) {
        Optional<String> s = Optional.ofNullable(super.remove(paramName)).map(String::valueOf);
        String sval = s.orElse(defaultStringValue);
        markMutation();
        return sval;
    }

    public int takeIntOrDefault(String paramName, int paramDefault) {
        Optional<String> i = Optional.ofNullable(super.remove(paramName)).map(String::valueOf);
        int ival = i.map(Integer::valueOf).orElse(paramDefault);
        markMutation();
        return ival;
    }

    public boolean takeBoolOrDefault(String paramName, boolean defaultBoolValue) {
        Optional<String> b = Optional.ofNullable(super.remove(paramName)).map(String::valueOf);
        boolean bval = b.map(Boolean::valueOf).orElse(defaultBoolValue);
        markMutation();
        return bval;
    }


    @Override
    public Object get(Object key) {
        logger.info("getting parameter " + key);
        return super.get(key);
    }

    public void set(String paramName, Object newValue) {
        super.put(paramName, String.valueOf(newValue));
        logger.info("parameter " + paramName + " set to " + newValue);
        markMutation();
    }

    private static Pattern encodedParamsPattern = Pattern.compile("(\\w+?)=(.+?);");

    @Override
    public Object put(String name, Object value) {
        Object oldVal = super.put(name, String.valueOf(value));
        logger.info("parameter " + name + " put to " + value);

        markMutation();
        return oldVal;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (Entry<? extends String, ? extends Object> entry : toMerge.entrySet()) {
            super.put(entry.getKey(),String.valueOf(entry.getValue()));
        }
        markMutation();
    }

    @Override
    public Object remove(Object key) {
        Object removed = super.remove(key);
        logger.info("parameter " + key + " removed");

        markMutation();
        return removed;
    }

    @Override
    public void clear() {
        logger.info("parameter map cleared:" + toString());
        super.clear();

        markMutation();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        logger.info("getting entry set for " + toString());
        return super.entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<String,Object>(e.getKey(), e.getValue()) {})
                .collect(Collectors.toCollection(HashSet::new));
    }


    private void markMutation() {
        changeCounter.incrementAndGet();
        logger.debug("calling " + listeners.size() + " listeners.");
        callListeners();
    }

    /**
     * Get the atomic change counter for this parameter map.
     * It getes incremented whenever any changes are made to the map.
     *
     * @return the atomic long change counter
     */
    public AtomicLong getChangeCounter() {
        return changeCounter;
    }

    public String toString() {
        return "(" + this.changeCounter.get() + ")/" + super.toString();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void callListeners() {
        for (Listener listener : listeners) {
            logger.info("calling listener:" + listener);
            listener.handleParameterMapUpdate(this);
        }
    }

    public int getSize() {
        return super.size();
    }

    public static ParameterMap parseOrException(String encodedParams) {
        if (encodedParams == null) {
            throw new RuntimeException("Must provide a non-null String to parse parameters.");
        }

        Matcher matcher = ParameterMap.encodedParamsPattern.matcher(encodedParams);

        LinkedHashMap<String, String> newParamMap = new LinkedHashMap<>();

        int lastEnd = 0;
        int triedAt = 0;

        while (matcher.find()) {
            triedAt = lastEnd;
            String paramName = matcher.group(1);
            String paramValueString = matcher.group(2);
            newParamMap.put(paramName, paramValueString);
            lastEnd = matcher.end();
        }

        if (lastEnd != encodedParams.length()) {
            throw new RuntimeException("unable to find pattern " + ParameterMap.encodedParamsPattern.pattern() + " at position " + triedAt + " in input" + encodedParams);
        }

        return new ParameterMap(newParamMap);
    }

    static Optional<ParameterMap> parseOptionalParams(Optional<String> optionalEncodedParams) {
        if (optionalEncodedParams.isPresent()) {
            return parseParams(optionalEncodedParams.get());
        }
        return Optional.empty();
    }

    public static Optional<ParameterMap> parseParams(String encodedParams) {
        try {
            return Optional.ofNullable(parseOrException(encodedParams));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Parse positional parameters, each suffixed with the ';' terminator.
     * This form simply allows for the initial parameter names to be elided, so long as they
     * are sure to match up with a well-known order. This method cleans up the input, injecting
     * the field names as necessary, and then calls the normal parsing logic.
     *
     * @param encodedParams     parameter string
     * @param defaultFieldNames the well-known field ordering
     * @return a new ParameterMap, if parsing was successful
     */
    public static ParameterMap parsePositional(String encodedParams, String[] defaultFieldNames) {

        String[] splitAtSemi = encodedParams.split(";");


        for (int wordidx = 0; wordidx < splitAtSemi.length; wordidx++) {

            if (!splitAtSemi[wordidx].contains("=")) {

                if (wordidx > (defaultFieldNames.length - 1)) {
                    throw new RuntimeException("positional param (without var=val; format) ran out of "
                            + "positional field names:"
                            + " names:" + Arrays.toString(defaultFieldNames)
                            + ", values: " + Arrays.toString(splitAtSemi)
                            + ", original: " + encodedParams
                    );
                }

                splitAtSemi[wordidx] = defaultFieldNames[wordidx] + "=" + splitAtSemi[wordidx] + ";";
            }
            if (!splitAtSemi[wordidx].endsWith(";")) {
                splitAtSemi[wordidx] = splitAtSemi[wordidx] + ";";
            }
        }

        String allArgs = Arrays.asList(splitAtSemi).stream().collect(Collectors.joining());
        ParameterMap parameterMap = ParameterMap.parseOrException(allArgs);
        return parameterMap;
    }

    public static interface Listener {
        void handleParameterMapUpdate(ParameterMap parameterMap);
    }

    public Map<String,String> getStringStringMap() {
        return new HashMap<String,String>() {{
            for (Entry entry : ParameterMap.this.entrySet()) {
                put(entry.getKey().toString(),entry.getValue().toString());
            }
        }};
    }

}