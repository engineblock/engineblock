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

package io.engineblock.markers.logger;

import io.engineblock.activityapi.cycletracking.CycleMarker;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCycleMarker implements CycleMarker {
    private final static Logger logger = LoggerFactory.getLogger(LoggingCycleMarker.class);

    private final ActivityDef def;
    private final long slot;
    private final ThreadLocal<StringBuilder> sb = ThreadLocal.withInitial(StringBuilder::new);

    public LoggingCycleMarker(ActivityDef def, long slot) {
        this.def = def;
        this.slot = slot;
    }

    @Override
    public synchronized void markResult(long completedCycle, int result) {
        sb.get().setLength(0);
        sb.get()
                .append("activity=").append(def.getAlias())
                .append(",cycle=").append(completedCycle)
                .append(",result=").append(result);
        logger.info(sb.get().toString());
    }
}
