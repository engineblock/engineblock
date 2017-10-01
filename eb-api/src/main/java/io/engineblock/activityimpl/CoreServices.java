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

package io.engineblock.activityimpl;

import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.OutputType;
import io.engineblock.activityapi.cycletracking.filters.IntPredicateDispenser;
import io.engineblock.activityapi.cycletracking.filters.ResultFilterType;
import io.engineblock.activityapi.input.InputDispenser;
import io.engineblock.activityapi.input.InputType;
import io.engineblock.activityapi.output.OutputDispenser;
import io.engineblock.util.SimpleConfig;

import java.util.Optional;

public class CoreServices {

    public static <A extends Activity> Optional<OutputDispenser> getOutputDispenser(A activity) {
        Optional<OutputDispenser> outputDispenser = new SimpleConfig(activity, "output").getString("type")
                .flatMap(OutputType.FINDER::get)
                .map(mt -> mt.getMarkerDispenser(activity));
        return outputDispenser;
    }


    public static <A extends Activity> Optional<IntPredicateDispenser> getResultFilterDispenser(A activity) {
        Optional<IntPredicateDispenser> intPredicateDispenser = new SimpleConfig(activity, "resultfilter")
                .getString("type")
                .flatMap(ResultFilterType.FINDER::get)
                .map(rft -> rft.getFilterDispenser(activity));
        return intPredicateDispenser;
    }

    public static <A extends Activity> InputDispenser getInputDispenser(A activity) {
        String inputTypeName = new SimpleConfig(activity, "input").getString("type").orElse("targetrate");
        InputType inputType = InputType.FINDER.getOrThrow(inputTypeName);
        InputDispenser dispenser = inputType.getInputDispenser(activity);
        return dispenser;
    }
}
