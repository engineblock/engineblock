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

package io.engineblock.markers.filebuffer;

import io.engineblock.activityapi.cycletracking.CycleSegment;
import io.engineblock.activityapi.cycletracking.Tracker;
import io.engineblock.activityimpl.ActivityDef;

import java.util.concurrent.atomic.AtomicLong;

public class FileBufferTracker implements Tracker {

    private final ActivityDef activityDef;
    FileBufferResultSink sink;
    FileBufferResultSource source;

    public FileBufferTracker(ActivityDef activityDef) {
        this.activityDef = activityDef;
        sink = new FileBufferResultSink(activityDef);
        source = new FileBufferResultSource(activityDef);
    }

    @Override
    public boolean markResult(long completedCycle, int result) {
        return sink.markResult(completedCycle, result);
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
}
