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

package io.engineblock.activityapi.cycletracking.markers.filebuffer;

import io.engineblock.activityapi.cycletracking.CycleSource;
import io.engineblock.activityapi.cycletracking.CycleSegment;
import io.engineblock.activityimpl.ActivityDef;

import java.util.concurrent.atomic.AtomicLong;

public class FileBufferCycleSource implements CycleSource {

    private ActivityDef activityDef;

    public FileBufferCycleSource(ActivityDef activityDef) {
        this.activityDef = activityDef;
    }

    @Override
    public AtomicLong getMinCycle() {
        return null;
    }

    @Override
    public AtomicLong getMaxCycle() {
        return null;
    }

    @Override
    public long getPendingCycle() {
        return 0;
    }

    @Override
    public long getCycleInterval(int stride) {
        return 0;
    }

    @Override
    public CycleSegment getSegment(int stride) {
        return null;
    }

    @Override
    public long remainingCycles() {
        throw new RuntimeException("implement me");
    }
}
