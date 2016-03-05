/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package com.metawiring.load.cycler;

import com.metawiring.load.activityapi.*;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.activitycore.motors.CoreActivityMotor;

/**
 * Produce index ActivityMotor instances with an input and action,
 * given the input and an action factory.
 */
public class CoreMotorDispenser implements MotorDispenser {

    private InputDispenser inputDispenser;
    private ActionDispenser actionDispenser;

    public CoreMotorDispenser(InputDispenser inputDispenser, ActionDispenser actionDispenser) {
        this.inputDispenser = inputDispenser;
        this.actionDispenser = actionDispenser;
    }

    @Override
    public ActivityMotor getMotor(ActivityDef activityDef, int slotId) {
        Action action = actionDispenser.getAction(slotId);
        Input input = inputDispenser.getInput(slotId);
        ActivityMotor am = new CoreActivityMotor(slotId,input,action);
        return am;
    }
}
