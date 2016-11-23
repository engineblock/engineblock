package io.engineblock.activityapi;

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;

/**
 * Provides the components needed to build and run an activity a runtime.
 */
public interface Activity extends Comparable<Activity> {

    MotorDispenser getMotorDispenser();

    void setMotorDispenser(MotorDispenser motorDispenser);

    InputDispenser getInputDispenser();

    void setInputDispenser(InputDispenser inputDispenser);

    ActionDispenser getActionDispenser();

    void setActionDispenser(ActionDispenser actionDispenser);

    ActivityDef getActivityDef();

    default String getAlias() {
        return getActivityDef().getAlias();
    }

    default ParameterMap getParams() {
        return getActivityDef().getParams();
    }

    default void initActivity() {
    }

    RunState getRunState();
    void setRunState(RunState runState);

    default void shutdownActivity() {
    }

    default String getCycleSummary() {
        return getActivityDef().getCycleSummary();
    }

}
