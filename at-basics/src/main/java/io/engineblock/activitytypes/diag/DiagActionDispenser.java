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
package io.engineblock.activities.diag;

import io.engineblock.activityapi.core.Action;
import io.engineblock.activityapi.core.ActionDispenser;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class DiagActionDispenser implements ActionDispenser {

    private final static Logger logger = getLogger(DiagActionDispenser.class);
    private DiagActivity activity;

    public DiagActionDispenser(DiagActivity activity) {
        this.activity = activity;
    }

    @Override
    public Action getAction(int slot) {
        if (activity.isAsync()) {
            logger.debug("creating new Async DiagAction instance for slot=" + slot + ", activity=" + activity);
            return new AsyncDiagAction(activity, slot);
        } else {
            logger.debug("creating new DiagAction instance for slot=" + slot + ", activity=" + activity);
            return new DiagAction(slot, activity.getActivityDef(), activity);
        }
    }
}
