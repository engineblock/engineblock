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

package io.engineblock.activityimpl.marker;

import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.cycletracking.CycleMarker;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the default cycle marker implementation for EngineBlock.
 *
 * This cycle marker wraps another tracking structure in order to
 * allow for flexible buffering methods.
 */
public class CoreMarker implements CycleMarker {

    private Activity activity;
    private LinkedList<MarkerExtent> extents = new LinkedList<>();
    private long extentSize=1024*1024;
    private AtomicLong baseCycle=new AtomicLong(0L);
    private AtomicLong contiguousSize=new AtomicLong(-1L);
    private AtomicReference<MarkerExtent> first;

    public CoreMarker(Activity activity) {
        this.activity = activity;
        this.extentSize = 1024;
        first.set(new MarkerExtent(0,extentSize));
    }

    @Override
    public void markResult(long completedCycle, int result) {
        MarkerExtent pointer = extents.getFirst();

    }
}
