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

package io.engineblock.activityapi.core;

import io.engineblock.activityapi.cyclelog.filters.IntPredicateDispenser;
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

    void setOutputDispenserDelegate(OutputDispenser outputDispenser);

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
