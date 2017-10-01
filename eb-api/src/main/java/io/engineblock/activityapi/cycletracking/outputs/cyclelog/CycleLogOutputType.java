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

package io.engineblock.activityapi.cycletracking.outputs.cyclelog;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.OutputType;
import io.engineblock.activityapi.output.Output;
import io.engineblock.activityapi.output.OutputDispenser;
import io.engineblock.activityimpl.marker.ConcurrentOutputSegmenter;

@AutoService(OutputType.class)
public class CycleLogOutputType implements OutputType {

    @Override
    public String getName() {
        return "cyclelog";
    }

    @Override
    public OutputDispenser getMarkerDispenser(Activity activity) {
        return new Dispenser(activity);
    }

    public static class Dispenser implements OutputDispenser {
        private final Output marker;
        private Activity activity;

        public Dispenser(Activity activity) {
            this.activity = activity;
            ConcurrentOutputSegmenter concurrentOutputSegmenter = new ConcurrentOutputSegmenter(activity);
            CycleLogOutput rlemarker = new CycleLogOutput(activity.getActivityDef());
            concurrentOutputSegmenter.addExtentReader(rlemarker);
            this.marker = concurrentOutputSegmenter;
            activity.registerAutoCloseable(marker);
        }

        @Override
        public Output getMarker(long slot) {
            return marker;
        }
    }
}
