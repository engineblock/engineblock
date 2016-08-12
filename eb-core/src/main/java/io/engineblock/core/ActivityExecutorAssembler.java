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
package io.engineblock.core;

import io.engineblock.activityapi.*;
import io.engineblock.activityimpl.ActivityDef;

/**
 * Controls the way that an activity type is used to create instances of an activity.
 */
public class ActivityExecutorAssembler {

    public static ActivityExecutor getExecutor(ActivityDef activityDef, ActivityType activityType) {

        Activity activity = activityType.getActivity(activityDef);
        InputDispenser inputDispenser = activityType.getInputDispenser(activity);
        activity.setInputDispenser(inputDispenser);

        ActionDispenser actionDispenser = activityType.getActionDispenser(activity);
        activity.setActionDispenser(actionDispenser);

        MotorDispenser motorDispenser = activityType.getMotorDispenser(activity,inputDispenser,actionDispenser);
        activity.setMotorDispenser(motorDispenser);

        ActivityExecutor executor = new ActivityExecutor(activity);
        return executor;
    }

}
