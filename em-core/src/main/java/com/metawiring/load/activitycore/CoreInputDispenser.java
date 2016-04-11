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
package com.metawiring.load.activitycore;

import com.metawiring.tools.activityapi.Input;
import com.metawiring.tools.activityapi.InputDispenser;
import com.metawiring.tools.activityapi.ActivityDef;

/**
 * An input dispenser that returns the same sequence supplier to all consumers.
 */
public class CoreInputDispenser implements InputDispenser {

    private final CoreInput input;

    public CoreInputDispenser(ActivityDef activityDef) {
        this.input = new CoreInput().setRange(
                activityDef.getParams().getLongOrDefault("min",1L),
                activityDef.getParams().getLongOrDefault("max",Long.MAX_VALUE)
        );
    }

    @Override
    public Input getInput(long slotId) {
        return input;
    }

    public String toString() {
        return "InputDispenser/" + input.toString();
    }
}
