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

import io.engineblock.activityapi.ActivityDefObserver;
import io.engineblock.activityapi.Input;
import io.engineblock.activityapi.Stoppable;
import io.engineblock.activityimpl.ActivityDef;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Thread.sleep;

public class LinkedInput implements Input, ActivityDefObserver, Stoppable {

    private AtomicLong cycleValue = new AtomicLong(0L);
    private long linkedPoint = 0L;
    private AtomicLong min = new AtomicLong(0L);
    private AtomicLong max = new AtomicLong(Long.MAX_VALUE);

    private Input linkedInput;
    private ActivityDef activityDef;
    private boolean running=true;

    public LinkedInput(ActivityDef activityDef, Input linkedInput) {
        this.activityDef = activityDef;
        this.linkedInput = linkedInput;
        this.cycleValue.set(linkedInput.getCurrent());
    }

    @Override
    public AtomicLong getMin() {
        return min;
    }

    @Override
    public AtomicLong getMax() {
        return max;
    }

    @Override
    public long getCurrent() {
        return cycleValue.get();
    }

    @Override
    public long getAsLong() {
        // Because this is a conditional increment, we have to use CAS to avoid race conditions invalidating
        // our invariant that this input never provides a higher value than the linked input

        while (true) {
            long current = cycleValue.get();
            if (current<linkedPoint) {
                long next = current + 1;
                if (cycleValue.compareAndSet(current, next)) {
                    return current;
                }
            }
            long newLinkedPoint = linkedInput.getCurrent();
            if (newLinkedPoint == linkedPoint) {
                slowMeDown(); // On if the linking input tried to go faster than the linked-to input
                if(!running) { return current; }
            } else {
                linkedPoint = newLinkedPoint;
                continue;
            }
        }
    }

    /**
     * for testing
     * @return true, if this input could advance according to the linked input
     */
    protected boolean canAdvance() {
           return (cycleValue.get() < linkedInput.getCurrent());
    }

    /**
     * This is really only to cause the thread to pause momentarily and to limit max idle churn to 1K/s
     * Using an active idle loop would result in more timely linking, but it would also be too wasteful
     * Some comparisons are needed to prove this out
     */
    private void slowMeDown() {
        try {
            sleep(1);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {

    }

    @Override
    public void requestStop() {
        this.running=false;
    }
}
