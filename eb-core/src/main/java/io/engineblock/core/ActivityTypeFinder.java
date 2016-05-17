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
package io.engineblock.core;

import io.engineblock.activityapi.ActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convenient singleton for accessing all loadable ActivityType instances.
 */
public class ActivityTypeFinder {

    private static final Logger logger = LoggerFactory.getLogger(ActivityTypeFinder.class);
    private static ActivityTypeFinder instance;

    private final Map<String, ActivityType> types = new ConcurrentHashMap<>();

    private ActivityTypeFinder() {
    }

    public synchronized static ActivityTypeFinder instance() {
        if (instance==null) {
            instance = new ActivityTypeFinder();
        }
        return instance;
    };

    /**
     * Return the named activity type, optionally.
     * @param activityTypeName The canonical activity type name.
     * @return an optional ActivityType instance
     */
    public Optional<ActivityType> get(String activityTypeName) {
        return Optional.ofNullable(getTypes().get(activityTypeName));
    }

    /**
     * Return the named activity type or throw an error.
     * @param activityType The canonical activity type name.
     * @return an ActivityType instance
     * @throws RuntimeException if the activity type isn't found.
     */
    public ActivityType getOrThrow(String activityType) {
        Optional<ActivityType> at = Optional.ofNullable(getTypes().get(activityType));
        return at.orElseThrow(
                () -> new RuntimeException("ActivityType '" + activityType + "' not found.")
        );
    }

    private synchronized Map<String, ActivityType> getTypes() {
        if (types.size()==0) {
            ClassLoader cl = getClass().getClassLoader();
            logger.debug("loading ActivityTypes");
            ServiceLoader<ActivityType> sl = ServiceLoader.load(ActivityType.class);
            for (ActivityType at : sl) {
                if (types.get(at.getName()) != null) {
                    throw new RuntimeException("ActivityType '" + at.getName()
                            + "' is already defined.");
                }
                types.put(at.getName(),at);
            }
        }
        logger.info("Loaded Types:" + types.keySet());
        return types;
    }

    /**
     * Return list of activity types tha have been found by this runtime,
     * in alphabetical order of their type names.
     * @return a list of ActivityType instances.
     */
    public List<ActivityType> getAll() {
        List<ActivityType> types = new ArrayList<>(getTypes().values());
        types.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        return Collections.unmodifiableList(types);
    }
}
