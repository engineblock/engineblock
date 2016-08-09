package io.engineblock.activityimpl;

import io.engineblock.activityapi.*;

/**
 * A default implementation of an Activity, suitable for building upon.
 */
public class SimpleActivity implements Activity {

    private MotorDispenser motorDispenser;
    private InputDispenser inputDispenser;
    private ActionDispenser actionDispenser;
    private ActivityDef activityDef;

    public SimpleActivity(ActivityDef activityDef) {

        this.motorDispenser = motorDispenser;
        this.inputDispenser = inputDispenser;
        this.actionDispenser = actionDispenser;
        this.activityDef = activityDef;
    }

    @Override
    public MotorDispenser getMotorDispenser() {
        return motorDispenser;
    }

    @Override
    public void setMotorDispenser(MotorDispenser motorDispenser) {
        this.motorDispenser = motorDispenser;
    }

    @Override
    public InputDispenser getInputDispenser() {
        return inputDispenser;
    }

    @Override
    public void setInputDispenser(InputDispenser inputDispenser) {
        this.inputDispenser = inputDispenser;
    }

    @Override
    public ActionDispenser getActionDispenser() {
        return actionDispenser;
    }

    @Override
    public void setActionDispenser(ActionDispenser actionDispenser) {
        this.actionDispenser = actionDispenser;
    }

    @Override
    public ActivityDef getActivityDef() {
        return activityDef;
    }

    public String toString() {
        return getAlias();
    }

    @Override
    public int compareTo(Activity o) {
        return getAlias().compareTo(o.getAlias());
    }
}
