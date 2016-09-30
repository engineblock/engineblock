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
package io.engineblock.activityimpl.input;

import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.Input;
import io.engineblock.activityapi.InputDispenser;
import io.engineblock.activityimpl.action.CoreActionDispenser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An input dispenser that returns the same rate-limiting sequence supplier to all consumers.
 */
public class SimpleInputDispenser implements InputDispenser {

    private final static Logger logger = LoggerFactory.getLogger(CoreActionDispenser.class);

    private final TargetRateInput input;

    public SimpleInputDispenser(Activity activity) {
        this.input = new TargetRateInput(activity.getActivityDef());
    }

    @Override
    public Input getInput(long slotId) {
        return input;
    }

    public String toString() {
        return "InputDispenser/" + input.toString();
    }
}
