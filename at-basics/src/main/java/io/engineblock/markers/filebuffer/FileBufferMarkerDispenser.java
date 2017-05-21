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

package io.engineblock.markers.filebuffer;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.cycletracking.MarkerDispenser;
import io.engineblock.activityimpl.ActivityDef;

// TODO: Create one dispenser per activity

@AutoService(MarkerDispenser.class)
public class FileBufferMarkerDispenser implements MarkerDispenser<FileBufferMarker> {

    @Override
    public String getName() {
        return "filebuffer";
    }

    @Override
    public FileBufferMarker getMarker(ActivityDef def, long slot) {
        return new FileBufferMarker(def);
    }
}
