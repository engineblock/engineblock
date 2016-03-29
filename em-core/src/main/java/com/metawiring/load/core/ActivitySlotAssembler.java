package com.metawiring.load.core;

import com.metawiring.load.activityapi.*;
import com.metawiring.load.activitycore.CoreActionDispenser;
import com.metawiring.load.activitycore.CoreInputDispenser;
import com.metawiring.load.config.ActivityDefImpl;
import com.metawiring.load.activitycore.CoreMotorDispenser;

/**
 * Given an ActivityDef instance and an ActivityType instance, create a dispenser for the Runnable
 * for each numbered slot in an activity.
 */
public class ActivitySlotAssembler {

    public static MotorDispenser resolveMotorDispenser(
            ActivityDef activityDef,
            ActivityType activityType) {

        InputDispenser inputDispenser = resolveInputDispenser(activityDef, activityType);
        ActionDispenser actionDispenser = resolveActionDispenser(activityDef, activityType);

        if (activityType instanceof MotorDispenserProvider) {
            return ((MotorDispenserProvider) activityType).getMotorDispenser(activityDef, inputDispenser, actionDispenser);
        } else {
            return new CoreMotorDispenser(inputDispenser, actionDispenser);
        }
    }

    private static ActionDispenser resolveActionDispenser(ActivityDef activityDef, ActivityType activityType) {
        if (activityType instanceof ActionDispenserProvider) {
            return ((ActionDispenserProvider) activityType).getActionDispenser(activityDef);
        } else {
            return new CoreActionDispenser(activityDef);
        }
    }

    private static InputDispenser resolveInputDispenser(ActivityDef activityDef, ActivityType activityType) {
        if (activityType instanceof InputDispenserProvider) {
            return ((InputDispenserProvider) activityType).getInputDispensor(activityDef);
        } else {
            return new CoreInputDispenser(activityDef);
        }
    }


}
