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
import io.engineblock.activityapi.cycletracking.Marker;
import io.engineblock.activityapi.cycletracking.MarkerDispenser;
import io.engineblock.activityapi.cycletracking.MarkerFinder;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CoreMarkerDispenser implements MarkerDispenser {
    private final static Logger logger = LoggerFactory.getLogger(CoreMarkerDispenser.class);

    private Activity activity;
    private Map<String, Activity> activities;
    private Marker marker;

    public CoreMarkerDispenser(Activity activity) {
        this.activity = activity;
    }

    @Override
    public String getName() {
        return "coremarker";
    }

    @Override
    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public Marker getMarker(long slot) {
        if (marker == null) {
            Optional<String> markerOption =
                    activity.getParams().getOptionalString("marker");
            if (markerOption.isPresent()) {
                MarkerConfig markerConfig = new MarkerConfig(activity.getActivityDef());
                String markerType = markerConfig.params.get("type");

                long cycleCount = activity.getActivityDef().getCycleCount();
                long stride = activity.getParams().getOptionalLong("stride").orElse(1L);
                if ((cycleCount%stride)!=0) {
                    throw new RuntimeException("stride must evenly divide into cycles.");
                    // TODO: Consider setting cycles to " ...
                }
                int extentSize = calculateExtentSize(cycleCount, stride);
                if (markerType==null) {
                    marker = new CoreMarker(
                            this.activity.getActivityDef().getStartCycle(),
                            this.activity.getActivityDef().getEndCycle(),
                            extentSize,
                            3
                    );
                } else {
                    Optional<MarkerDispenser> trackerDispenser = MarkerFinder.instance().get(markerType);
                    marker = trackerDispenser.map(md -> md.getMarker(slot)).orElseThrow(
                            () -> new RuntimeException("Unable to find tracker implementation for " + markerType)
                    );
                }

            }
        }
        return marker;
    }

    private int calculateExtentSize(long cycleCount, long stride) {
        if (cycleCount<=2000000) {
            return (int) cycleCount;
        }
        for (int cs=2000000;  cs>500000;  cs--) {
            if ((cycleCount%cs)==0 && (cs%stride)==0) {
                return cs;
            }
        }
        throw new RuntimeException("no even divisor of cycleCount and Stride between 500K and 2M, with cycles=" + cycleCount +",  and stride=" + stride);
    }

    private class MarkerConfig {
        final Map<String, String> params;

        public MarkerConfig(ActivityDef activityDef) {
            Optional<String> marker = activityDef.getParams().getOptionalString("marker");
            marker.orElseThrow(() -> new RuntimeException("marker parameter is missing?"));
            logger.debug("parsing marker config:" + marker.get());
            params =
                    Arrays.stream(marker.get().split(","))
                            .map(s -> s.split("="))
                            .collect(Collectors.toMap(o -> o[0], o -> o[1]));
        }

    }

}
