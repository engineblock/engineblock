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

package io.engineblock.activityapi;

import io.engineblock.activityimpl.*;

/**
 * <p>An ActivityType is the central extension point in EngineBlock for new
 * activity types drivers. It is responsible for naming the activity type, as well as providing
 * the input, activity, and motor instances that will be assembled into an activity.</p>
 * <p>At the very minimum, a useful implementation of an activity type should provide
 * an action dispenser. Default implementations of input and motor dispensers are provided,
 * and by extension, default inputs and motors.</p>
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
     * Create an instance of an activity from the activity type.
     *
     * @param activityDef the definition that initializes and controls the activity.
     * @return a distinct Activity instance fr each call
     */
    default Activity getActivity(ActivityDef activityDef) {
        return new SimpleActivity(activityDef);
    }

    /**
     * This method will be called <em>once</em> per action instance.
     *
     * @param activity The activity instance that will parameterize the returned ActionDispenser instance.
     * @return an instance of ActionDispenser
     */
    default ActionDispenser getActionDispenser(Activity activity) {
        return new CoreActionDispenser(activity);
    }

    /**
     * Return the InputDispenser instance that will be used by the associated activity to create Input factories
     * for each thread slot.
     *
     * @param activity the Activity instance which will parameterize this InputDispenser
     * @return the InputDispenser for the associated activity
     */
    default InputDispenser getInputDispenser(Activity activity) {
        return new CoreInputDispenser(activity);
    }

    default MotorDispenser getMotorDispenser(
            Activity activity,
            InputDispenser inputDispenser,
            ActionDispenser actionDispenser) {
        return new CoreMotorDispenser(activity, inputDispenser, actionDispenser);
    }


}
