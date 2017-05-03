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

import io.engineblock.activityapi.ActivitiesAware;
import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.Input;
import io.engineblock.activityapi.InputDispenser;

import java.util.Map;

/**
 * This dispenser assumes one input per activity. It will choose an appropriate input implementation
 * depending on the activity parameters.
 *
 * Presently, it supports:
 * <UL>
 *     <LI>{@link TargetRateInput} - selected by use of the parameter <em>targetrate</em></LI>
 *     <LI>{@link LinkedInput} - selected by use of the parameter <em>linkinput</em></LI>
 * </UL>
 *
 * See the respective javadoc on those classes for more dteails.
 */
public class CoreInputDispenser implements InputDispenser, ActivitiesAware {

    private Activity activity;
    private Map<String, Activity> activities;
    private Input input;

    public CoreInputDispenser(Activity activity) {
        this.activity = activity;
    }

    @Override
    public Input getInput(long slot) {
        if (this.input == null) {
            this.input = createInput(slot);
        }
        return input;
    }

    private synchronized Input createInput(long slot) {
        String linkinput = activity.getParams().getOptionalString("linkinput").orElse("");
        Input input=null;
        if (linkinput.isEmpty()) {
            input =new TargetRateInput(activity.getActivityDef());
        } else {
            Activity linkedActivity = activities.get(linkinput);
            if (linkedActivity==null) {
                throw new RuntimeException("To link input of activity " + activity.getAlias() + " to the input of " +
                linkinput +", it first has to exist. Create non-linked activities first.");
            }
            input = new LinkedInput(
                    activity.getActivityDef(),
                    linkedActivity.getInputDispenserDelegate().getInput(slot)
            );
        }
        return input;
    }

    @Override
    public void setActivitiesMap(Map<String, Activity> activities) {
        this.activities = activities;
    }
}
