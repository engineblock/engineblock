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

package io.engineblock.activityapi.cyclelog.outputs.logger;

import io.engineblock.activityapi.cyclelog.buffers.results.ResultReadable;
import io.engineblock.activityapi.cyclelog.inputs.cyclelog.CanFilterResultValue;
import io.engineblock.activityapi.output.Output;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

public class LoggingOutput implements Output,CanFilterResultValue {
    private final static Logger logger = LoggerFactory.getLogger(LoggingOutput.class);

    private final ActivityDef def;
    private final long slot;
    private final ThreadLocal<StringBuilder> sb = ThreadLocal.withInitial(StringBuilder::new);
    private Predicate<ResultReadable> filter;

    public LoggingOutput(ActivityDef def, long slot) {
        this.def = def;
        this.slot = slot;
    }

    @Override
    public boolean onCycleResult(long completedCycle, int result) {
        if (filter!=null && !filter.test(new ResultReadableWrapper(result))) {
            return true;
        }
        sb.get().setLength(0);
        sb.get()
                .append("activity=").append(def.getAlias())
                .append(",cycle=").append(completedCycle)
                .append(",result=").append((byte) (result & 127));
        logger.info(sb.get().toString());
        return true;
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void setFilter(Predicate<ResultReadable> filter) {
        this.filter = filter;
    }

    private static class ResultReadableWrapper implements ResultReadable {
        private int result;
        public ResultReadableWrapper(int result) {
            this.result = result;
        }
        public int getResult() { return result; }
    }


}
