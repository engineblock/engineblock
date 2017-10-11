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
package io.engineblock.activityimpl.motor;

import io.engineblock.activityapi.core.*;
import io.engineblock.activityapi.input.Input;
import io.engineblock.activityapi.input.InputDispenser;
import io.engineblock.activityapi.output.Output;
import io.engineblock.activityapi.output.OutputDispenser;
import io.engineblock.activityimpl.ActivityDef;

import java.util.function.IntPredicate;

/**
 * Produce index ActivityMotor instances with an input and action,
 * given the input and an action factory.
 */
public class CoreMotorDispenser implements MotorDispenser {

    private final Activity activity;
    private InputDispenser inputDispenser;
    private ActionDispenser actionDispenser;
    private OutputDispenser markerDispenser;

    public CoreMotorDispenser(Activity activity,
                              InputDispenser inputDispenser,
                              ActionDispenser actionDispenser,
                              OutputDispenser markerDispenser
                              ) {
        this.activity = activity;
        this.inputDispenser = inputDispenser;
        this.actionDispenser = actionDispenser;
        this.markerDispenser = markerDispenser;
    }

    @Override
    public Motor getMotor(ActivityDef activityDef, int slotId) {
        Action action = actionDispenser.getAction(slotId);
        Input input = inputDispenser.getInput(slotId);
        Output output = null;
        if (markerDispenser!=null) {
            output = markerDispenser.getOutput(slotId);
        }
        IntPredicate resultFilter = null;
        Motor am = new CoreMotor(activity.getActivityDef(), slotId, input, action, output);
        return am;
    }
}
