/*
*   Copyright 2015 jshook
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
package io.engineblock.activitycore;

import com.codahale.metrics.Timer;
import io.engineblock.activityapi.*;
import io.engineblock.core.MetricsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;
import static io.engineblock.activityapi.SlotState.*;

/**
 * <p>ActivityMotor is a Runnable which runs in one of an activity's many threads.
 * It is the iteration harness for individual cycles of an activity. Each ActivityMotor
 * instance is responsible for taking input from a LongSupplier and applying
 * the provided LongConsumer to it on each cycle. These two parameters are called
 * input and action, respectively.
 * </p>
 */
public class CoreMotor implements ActivityDefObserver, Motor {

    private static final Logger logger = LoggerFactory.getLogger(CoreMotor.class);
    private final AtomicReference<SlotState> slotState = new AtomicReference<>(SlotState.Initialized);
    private long slotId;
    private Input input;
    private Action action;
    private Timer timer;
    private String metricsName;

    /**
     * Create an ActivityMotor.
     *
     * @param metricsName The base name for all metrics specific to this Motor's activity.
     * @param slotId The enumeration of the motor, as assigned by its executor.
     * @param input   A LongSupplier which provides the cycle number inputs.
     */
    public CoreMotor(
            String metricsName,
            long slotId,
            Input input) {
        this.slotId = slotId;
        setInput(input);
        this.metricsName = metricsName;
    }

    /**
     * Create an ActivityMotor.
     *
     * @param metricsName The base name for all metrics specific to this Motor's activity.
     * @param slotId The enumeration of the motor, as assigned by its executor.
     * @param input   A LongSupplier which provides the cycle number inputs.
     * @param action  An LongConsumer which is applied to the input for each cycle.
     */
    public CoreMotor(
            String metricsName, long slotId,
            Input input,
            Action action
    ) {
        this(metricsName, slotId, input);
        setAction(action);
    }

    /**
     * Set the input for this ActivityMotor.
     *
     * @param input The LongSupplier that provides the cycle number.
     * @return this ActivityMotor, for chaining
     */
    @Override
    public Motor setInput(Input input) {
        this.input = input;
        return this;
    }

    @Override
    public Input getInput() {
        return input;
    }


    /**
     * Set the action for this ActivityMotor.
     *
     * @param action The LongConsumer that will be applied to the next cycle number.
     * @return this ActivityMotor, for chaining
     */
    @Override
    public Motor setAction(Action action) {
        this.action = action;
        return this;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public long getSlotId() {
        return this.slotId;
    }


    @Override
    public void run() {

        timer = MetricsContext.metrics().timer("activity." + metricsName + ".timer");

        if (slotState.get()==Finished) {
            logger.warn("input was already exhausted for slot " + slotId + ", cycling back to finished");
        }

        enterState(Started);
        long cyclenum;
        long cycleMax = input.getMax();


        if (action instanceof ActionInitializer) {
            ((ActionInitializer)action).init();
        }

        while (slotState.get() == Started) {
            Timer.Context cycleTime = timer.time();

            cyclenum = input.getAsLong();
            if (cyclenum > cycleMax) {
                logger.debug("input exhausted (input " + cyclenum + "), stopping motor thread " + slotId);
                enterState(Finished);
                continue;
            }
            logger.trace("cycle " + cyclenum);
            action.accept(cyclenum);
            cycleTime.stop();
        }

        if (slotState.get()==Stopping) {
            enterState(Stopped);
        }
    }


    @Override
    public String toString() {
        return "slot:" + this.slotId + "; state:" + slotState.get();
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        if (input instanceof ActivityDefObserver) {
            ((ActivityDefObserver) input).onActivityDefUpdate(activityDef);
        }
        if (action instanceof ActivityDefObserver) {
            ((ActivityDefObserver) action).onActivityDefUpdate(activityDef);
        }
    }

    @Override
    public synchronized void requestStop() {
        if (slotState.get()==Started) {
            enterState(SlotState.Stopping);
        }
    }

    @Override
    public SlotState getSlotState() {
        return slotState.get();
    }

    /**
     * <p>Transition the thread slot to a new state. only accepting valid transitions.</p>
     * <p>The valid slot states will be moved to a data type eventually, simplifying this method.</p>
     *
     * @param to The next SlotState for this thread/slot/motor
     */
    private synchronized void enterState(SlotState to) {
        SlotState from = slotState.get();
        if (!from.isValid(from, to)) {
            throw new RuntimeException("Invalid transition from " + from + " to " + to);
        }
        slotState.compareAndSet(from, to);
    }

}
