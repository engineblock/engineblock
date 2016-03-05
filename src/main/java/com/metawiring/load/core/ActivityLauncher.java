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
package com.metawiring.load.core;

import com.metawiring.load.activityapi.*;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.ActivityExecutor;

/**
 * Controls the way that an activity type is used to create instances of an activity.
 */
public class ActivityLauncher {

    public static ActivityExecutor getExecutor(ActivityDef activityDef, ActivityType activityType) {

        ActivityExecutor executor = new ActivityExecutor(activityDef);
        MotorDispenser motorDispenser = activityType.getMotorDispenser(activityDef);
        executor.setActivityMotorDispenser(motorDispenser);
        return executor;
    }
}
