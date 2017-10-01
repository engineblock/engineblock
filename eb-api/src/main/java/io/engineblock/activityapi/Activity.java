package io.engineblock.activityapi;

import io.engineblock.activityapi.cycletracking.filters.IntPredicateDispenser;
import io.engineblock.activityapi.output.OutputDispenser;
import io.engineblock.activityapi.input.InputDispenser;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.SimpleActivity;

/**
 * Provides the components needed to build and run an activity a runtime.
 * The easiest way to build a useful Activity is to extend {@link SimpleActivity}.
 */
public interface Activity extends Comparable<Activity> {

    /**
     * Register an object which should be closed after this activity is shutdown.
     *
     * @param closeable An Autocloseable object
     */
    void registerAutoCloseable(AutoCloseable closeable);

    /**
     * Close all autocloseables that have been registered with this Activity.
     */
    void closeAutoCloseables();

    MotorDispenser getMotorDispenserDelegate();

    void setMotorDispenserDelegate(MotorDispenser motorDispenser);

    InputDispenser getInputDispenserDelegate();

    void setInputDispenserDelegate(InputDispenser inputDispenser);

    ActionDispenser getActionDispenserDelegate();

    void setActionDispenserDelegate(ActionDispenser actionDispenser);

    IntPredicateDispenser getResultFilterDispenserDelegate();

    void setResultFilterDispenserDelegate(IntPredicateDispenser resultFilterDispenser);

    OutputDispenser getMarkerDispenserDelegate();

    void setMarkerDispenserDelegate(OutputDispenser markerDispenser);

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
