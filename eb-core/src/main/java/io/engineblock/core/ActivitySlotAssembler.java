package io.engineblock.core;

import io.engineblock.activityimpl.CoreActionDispenser;
import io.engineblock.activityimpl.CoreInputDispenser;
import io.engineblock.activityimpl.CoreMotorDispenser;
import io.engineblock.activityapi.*;
import io.engineblock.activityimpl.ActivityDef;

/**
 * Given an ActivityDef instance and an ActivityType instance, create a dispenser for the Runnable
 * for each numbered slot in an activity.
 */
public class ActivitySlotAssembler {

    public static MotorDispenser resolveMotorDispenser(
            ActivityDef activityDef,
            ActivityType activityType,
            InputDispenser inputDispenser,
            ActionDispenser actionDispenser) {
        if (activityType instanceof MotorDispenserProvider) {
            return ((MotorDispenserProvider) activityType).getMotorDispenser(activityDef, inputDispenser, actionDispenser);
        } else {
            return new CoreMotorDispenser(activityDef, inputDispenser, actionDispenser);
        }
    }

    public static ActionDispenser resolveActionDispenser(ActivityDef activityDef, ActivityType activityType) {
        if (activityType instanceof ActionDispenserProvider) {
            return ((ActionDispenserProvider) activityType).getActionDispenser(activityDef);
        } else {
            return new CoreActionDispenser(activityDef);
        }
    }

    public static InputDispenser resolveInputDispenser(ActivityDef activityDef, ActivityType activityType) {
        if (activityType instanceof InputDispenserProvider) {
            return ((InputDispenserProvider) activityType).getInputDispensor(activityDef);
        } else {
            return new CoreInputDispenser(activityDef);
        }
    }

}
