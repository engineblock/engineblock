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

import io.engineblock.activityapi.*;
import io.engineblock.activityapi.cycletracking.CycleSinkSource;
import io.engineblock.activityapi.cycletracking.TrackerDispenser;
import io.engineblock.activityimpl.ActivityDef;

/**
 * Produce index ActivityMotor instances with an input and action,
 * given the input and an action factory.
 */
public class CoreMotorDispenser implements MotorDispenser {

    private final Activity activity;
    private InputDispenser inputDispenser;
    private ActionDispenser actionDispenser;
    private TrackerDispenser trackerDispenser;

    public CoreMotorDispenser(Activity activity,
                              InputDispenser inputDispenser,
                              ActionDispenser actionDispenser,
                              TrackerDispenser trackerDispenser) {
        this.activity = activity;
        this.inputDispenser = inputDispenser;
        this.actionDispenser = actionDispenser;
        this.trackerDispenser = trackerDispenser;
    }

    @Override
    public Motor getMotor(ActivityDef activityDef, int slotId) {
        Action action = actionDispenser.getAction(slotId);
        Input input = inputDispenser.getInput(slotId);
        CycleSinkSource cycleSinkSource = trackerDispenser.getTracker(slotId);
        Motor am = new CoreMotor(activity.getActivityDef(), slotId, input, action, cycleSinkSource);
        return am;
    }
}
