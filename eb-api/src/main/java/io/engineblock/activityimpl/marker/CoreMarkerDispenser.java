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

package io.engineblock.activityimpl.marker;

import io.engineblock.activityapi.ActivitiesAware;
import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.cycletracking.CycleMarker;
import io.engineblock.activityapi.cycletracking.MarkerDispenser;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.extensions.CycleMarkerFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CoreMarkerDispenser implements MarkerDispenser, ActivitiesAware {
    private final static Logger logger = LoggerFactory.getLogger(CoreMarkerDispenser.class);


    private final Activity activity;
    private Map<String, Activity> activities;
    private CycleMarker marker;

    public CoreMarkerDispenser(Activity activity) {
        this.activity = activity;
    }

    @Override
    public String getName() {
        return "coremarkerdispenser";
    }

    @Override
    public CycleMarker getMarker(long slot) {
        if (marker == null) {
            Optional<String> markerOption =
                    activity.getParams().getOptionalString("marker");
            if (markerOption.isPresent()) {
                Config config = new Config(activity.getActivityDef());)
                String markerType = config.params.get("type");
                if (markerType==null) {
                    marker = new CoreMarker(activity);
                } else {
                    Optional<MarkerDispenser> markerDispenser = CycleMarkerFinder.instance().get(markerType);
                    marker = markerDispenser.map(md -> md.getMarker(slot)).orElseThrow(
                            () -> new RuntimeException("Unable to find marker implementation for " + markerType)
                    );
                }

            }
        }
        return marker;
    }

    @Override
    public void setActivitiesMap(Map<String, Activity> activities) {
        this.activities = activities;
    }

    private class Config {
        final Map<String, String> params;

        public Config(ActivityDef activityDef) {
            Optional<String> marker = activityDef.getParams().getOptionalString("marker");
            marker.orElseThrow(() -> new RuntimeException("marker parameter is missing?"));
            logger.debug("parsing marker config:" + marker.get());
            params =
                    Arrays.stream(marker.get().split(",", 2)[1].split(","))
                            .map(s -> s.split("="))
                            .collect(Collectors.toMap(o -> o[0], o -> o[1]));
        }

    }

}
