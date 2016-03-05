/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.activityapi;

import com.metawiring.load.activitycore.CoreInputDispenser;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.CoreMotorDispenser;

/**
 * <p>An ActivityType is the central extension point in TestClient for new types of
 * activitytypes. It is responsible for naming the activity type, as well as providing
 * the input, activity, and motor instances that will be assembled into an activity.
 * At the very minimum, an implementation of an activity type must provide
 * an action dispenser. Default implementations of input and motor dispensers are provided,
 * and by extension, default inputs and motors.
 *
 * If you want to control the internal assembly of an activity at runtime, you may
 * override the default implementations of the input an/or motor.
 *
 * <p>{@link InputDispenser} - An input dispenser controls how input instances are created
 * for each slot in an activity.</p>
 *
 * <p>{@link ActionDispenser} - An action dispenser controls how action instances are created
 * for each slot in an activity.</p>
 *
 * <p>{@link MotorDispenser} - This interface allows for control of the per-thread
 * execution harness which takes inputs and applies action to them.</p>
 *
 */
public interface ActivityType {
    /**
     * Return the short name of this activity type. The fully qualified name of an activity type is
     * this value, prefixed by the package of the implementing class.
     *
     * @return An activity type name, like "diag"
     */
    String getName();

    /**
     * Provide a way to generate instances of an action. The action should be
     *
     * @return an action factory
     */
    <T extends ActivityDef> ActionDispenser getActionDispenser(T activityDef);

    default InputDispenser getInputDispenser(ActivityDef activityDef) {
        return new CoreInputDispenser(activityDef);
    }

    default MotorDispenser getMotorDispenser(ActivityDef activityDef) {
        return new CoreMotorDispenser(
                getInputDispenser(activityDef),
                getActionDispenser(activityDef)
        );
    }

}
