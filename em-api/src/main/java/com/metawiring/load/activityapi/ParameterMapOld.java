package com.metawiring.load.activityapi;

import javax.script.Bindings;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public interface ParameterMapOld extends Map<String, Object>,ConcurrentMap<String, Object>,Serializable, Bindings {

    long getLongOrDefault(String paramName, long defaultLongValue);

    double getDoubleOrDefault(String paramName, double defaultDoubleValue);

    String getStringOrDefault(String paramName, String defaultStringValue);

    Optional<String> getOptionalString(String paramName);

    Optional<Long> getOptionalLong(String paramName);

    Optional<Double> getOptionalDouble(String paramName);

    Optional<Boolean> getOptionalBoolean(String paramName);

    int getIntOrDefault(String paramName, int defaultIntValue);

    boolean getBoolOrDefault(String paramName, boolean defaultBoolValue);

    Long takeLongOrDefault(String paramName, Long defaultLongValue);

    Double takeDoubleOrDefault(String paramName, double defaultDoubleValue);

    String takeStringOrDefault(String paramName, String defaultStringValue);

    int takeIntOrDefault(String paramName, int paramDefault);

    boolean takeBoolOrDefault(String paramName, boolean defaultBoolValue);

    void set(String paramName, Object newValue);

    @Override
    Object put(String name, Object value);

    @Override
    void putAll(Map<? extends String, ?> toMerge);

    @Override
    void clear();

    AtomicLong getChangeCounter();

    void addListener(Listener listener);

    void removeListener(Listener listener);

    int getSize();

    public static interface Listener {
        void handleParameterMapUpdate(ParameterMap parameterMap);
    }
}
