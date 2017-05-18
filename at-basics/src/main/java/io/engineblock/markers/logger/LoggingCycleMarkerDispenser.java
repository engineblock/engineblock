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

package io.engineblock.markers.logger;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.cycletracking.CycleMarker;
import io.engineblock.activityapi.cycletracking.CycleMarkerDispenser;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(CycleMarkerDispenser.class)
public class LoggingCycleMarkerDispenser implements CycleMarkerDispenser {

    private final static Logger logger = LoggerFactory.getLogger(LoggingCycleMarkerDispenser.class);

    @Override
    public String getName() {
        return "logger";
    }

    @Override
    public CycleMarker getMarker(ActivityDef def, long slot) {
        return new LoggingCycleMarker(def,slot);
    }
}
