package io.engineblock.activityapi;

import io.engineblock.activityapi.cycletracking.MarkerDispenser;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.SimpleActivity;

/**
 * Provides the components needed to build and run an activity a runtime.
 * The easiest way to build a useful Activity is to extend {@link SimpleActivity}.
 */
public interface Activity extends Comparable<Activity> {

    MotorDispenser getMotorDispenserDelegate();

    void setMotorDispenserDelegate(MotorDispenser motorDispenser);

    InputDispenser getInputDispenserDelegate();

    void setInputDispenserDelegate(InputDispenser inputDispenser);

    ActionDispenser getActionDispenserDelegate();

    void setActionDispenserDelegate(ActionDispenser actionDispenser);

    MarkerDispenser getCycleMarkerDispenserDelegate();

    void setMarkerDispenserDelegate(MarkerDispenser markerDispenser);

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
