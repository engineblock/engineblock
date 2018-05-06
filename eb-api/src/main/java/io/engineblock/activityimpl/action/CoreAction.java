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

package io.engineblock.activityimpl.action;

import io.engineblock.activityapi.core.SyncAction;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreAction implements SyncAction {
    private final static Logger logger = LoggerFactory.getLogger(CoreAction.class);

    private final int interval;
    private final int slot;
    private final ActivityDef activityDef;

    public CoreAction(ActivityDef activityDef, int slot) {
        this.activityDef = activityDef;
        this.slot = slot;
        this.interval = activityDef.getParams().getOptionalInteger("interval").orElse(1000);
    }

    @Override
    public int runCycle(long value) {
        if ((value % interval) == 0) {
            logger.info(activityDef.getAlias() + "[" + slot + "]: cycle=" + value);
        } else {
            logger.trace(activityDef.getAlias() + "[" + slot + "]: cycle=" + value);
        }
        return 0;
    }
}
