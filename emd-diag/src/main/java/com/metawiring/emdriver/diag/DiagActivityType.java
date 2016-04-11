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
package com.metawiring.emdriver.diag;

import com.google.auto.service.AutoService;
import com.metawiring.tools.activityapi.ActionDispenser;
import com.metawiring.tools.activityapi.ActionDispenserProvider;
import com.metawiring.tools.activityapi.ActivityDef;
import com.metawiring.tools.activityapi.ActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DiagActivty, aka "diag", is simply a diagnostic activity.
 * It logs the input to priority INFO on some interval, in milliseconds.
 * Each interval, one of the activitytypes will report both the current input value and
 * the number of milliseconds that have elapsed since the activity was scheduled to report.
 *
 * It is built-in to the core TestClient codebase, and always available for sanity checks.
 * It is also the default activity that is selected if no activity type is specified nor inferred.
 * It serves as a basic template for implementing your own activity type.
 */
@AutoService(ActivityType.class)
public class DiagActivityType implements ActivityType, ActionDispenserProvider {

    private static final Logger logger = LoggerFactory.getLogger(DiagActivityType.class);

    @Override
    public String getName() {
        return "diag";
    }

    @Override
    public ActionDispenser getActionDispenser(ActivityDef activityDef) {
        return new DiagActionDispenser(activityDef);
    }
}
