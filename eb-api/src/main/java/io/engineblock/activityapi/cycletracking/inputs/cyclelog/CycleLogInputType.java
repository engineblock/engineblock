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

package io.engineblock.activityapi.cycletracking.inputs.cyclelog;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.input.Input;
import io.engineblock.activityapi.input.InputDispenser;
import io.engineblock.activityapi.input.InputType;

@AutoService(InputType.class)
public class CycleLogInputType implements InputType {
    @Override
    public String getName() {
        return "cyclelog";
    }

    @Override
    public InputDispenser getInputDispenser(Activity activity) {
        return new Dispenser(activity);
    }

    public static class Dispenser implements InputDispenser {

        private final Activity activity;
        private final Input input;

        public Dispenser(Activity activity) {
            this.activity = activity;
            this.input = new CycleLogReader(activity);
        }

        @Override
        public Input getInput(long slot) {
            return input;
        }
    }

}
