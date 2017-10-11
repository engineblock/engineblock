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

package io.engineblock.activityapi.cyclelog.outputs.cyclelog;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.core.Activity;
import io.engineblock.activityapi.output.OutputType;
import io.engineblock.activityapi.cyclelog.outputs.ReorderingConcurrentResultBuffer;
import io.engineblock.activityapi.input.Input;
import io.engineblock.activityapi.output.Output;
import io.engineblock.activityapi.output.OutputDispenser;
import io.engineblock.activityimpl.marker.ContiguousOutputChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(OutputType.class)
public class CycleLogOutputType implements OutputType {

    @Override
    public String getName() {
        return "cyclelog";
    }

    @Override
    public OutputDispenser getMarkerDispenser(Activity activity) {
        return new Dispenser(activity);
    }

    public static class Dispenser implements OutputDispenser {
        private final static Logger logger = LoggerFactory.getLogger(OutputDispenser.class);

        private final Output output;
        private Activity activity;

        public Dispenser(Activity activity) {
            this.activity = activity;
            Input input = activity.getInputDispenserDelegate().getInput(0);
            CycleLogOutput rleFileWriter = new CycleLogOutput(activity);
            if (input.isContiguous()) {
                logger.debug("pre-buffering output extents contiguously before RLE buffering");
                ContiguousOutputChunker contiguousOutputChunker = new ContiguousOutputChunker(activity);
                contiguousOutputChunker.addExtentReader(rleFileWriter);
                this.output = contiguousOutputChunker;
            }
            else {
                logger.debug("pre-buffering output extends with best-effort before RLE buffering");
                ReorderingConcurrentResultBuffer prebuffer =
                        new ReorderingConcurrentResultBuffer(rleFileWriter,1000);
                this.output=prebuffer;
            }
            activity.registerAutoCloseable(output);
        }

        @Override
        public Output getOutput(long slot) {
            return output;
        }
    }
}
