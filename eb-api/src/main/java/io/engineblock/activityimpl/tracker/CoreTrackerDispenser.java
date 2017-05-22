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

package io.engineblock.activityimpl.tracker;

import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.cycletracking.Tracker;
import io.engineblock.activityapi.cycletracking.TrackerDispenser;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.extensions.CycleMarkerFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CoreTrackerDispenser implements TrackerDispenser {
    private final static Logger logger = LoggerFactory.getLogger(CoreTrackerDispenser.class);


    private Activity activity;
    private Map<String, Activity> activities;
    private Tracker tracker;

    public CoreTrackerDispenser(Activity activity) {
        this.activity = activity;
    }

    @Override
    public String getName() {
        return "coremarkerdispenser";
    }

    @Override
    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public Tracker getTracker(long slot) {
        if (tracker == null) {
            Optional<String> markerOption =
                    activity.getParams().getOptionalString("tracker");
            if (markerOption.isPresent()) {
                Config config = new Config(activity.getActivityDef());
                String trackerType = config.params.get("type");
                if (trackerType==null) {
                    tracker = new CoreTracker(
                            this.activity.getActivityDef().getStartCycle(),
                            this.activity.getActivityDef().getEndCycle(),
                            1024
                    );
                } else {
                    Optional<TrackerDispenser> markerDispenser = CycleMarkerFinder.instance().get(trackerType);
                    tracker = markerDispenser.map(md -> md.getTracker(slot)).orElseThrow(
                            () -> new RuntimeException("Unable to find tracker implementation for " + trackerType)
                    );
                }

            }
        }
        return tracker;
    }

    private class Config {
        final Map<String, String> params;

        public Config(ActivityDef activityDef) {
            Optional<String> marker = activityDef.getParams().getOptionalString("tracker");
            marker.orElseThrow(() -> new RuntimeException("tracker parameter is missing?"));
            logger.debug("parsing tracker config:" + marker.get());
            params =
                    Arrays.stream(marker.get().split(","))
                            .map(s -> s.split("="))
                            .collect(Collectors.toMap(o -> o[0], o -> o[1]));
        }

    }

}
