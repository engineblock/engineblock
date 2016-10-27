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
package io.engineblock.activityimpl.motor;

import com.codahale.metrics.Timer;
import io.engineblock.activityapi.*;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.ActivityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
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
public class CoreMotor implements ActivityDefObserver, Motor, Stoppable {

    private static final Logger logger = LoggerFactory.getLogger(CoreMotor.class);
    private long slotId;
    private Input input;
    private Action action;
    private Timer timer;
    private ActivityDef activityDef;
    private SlotStateTracker slotStateTracker;
    private AtomicReference<SlotState> slotState;

    /**
     * Create an ActivityMotor.
     *
     * @param activityDef The activity def that this motor will be associated with.
     * @param slotId      The enumeration of the motor, as assigned by its executor.
     * @param input       A LongSupplier which provides the cycle number inputs.
     */
    public CoreMotor(
            ActivityDef activityDef,
            long slotId,
            Input input) {
        this.activityDef = activityDef;
        this.slotId = slotId;
        setInput(input);
        slotStateTracker = new SlotStateTracker(slotId);
        slotState = slotStateTracker.getAtomicSlotState();
    }

    /**
     * Create an ActivityMotor.
     *
     * @param activityDef The activity def that this motor is based on.
     * @param slotId      The enumeration of the motor, as assigned by its executor.
     * @param input       A LongSupplier which provides the cycle number inputs.
     * @param action      An LongConsumer which is applied to the input for each cycle.
     */
    public CoreMotor(
            ActivityDef activityDef,
            long slotId,
            Input input,
            Action action
    ) {
        this(activityDef, slotId, input);
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
    public SlotStateTracker getSlotStateTracker() {
        return slotStateTracker;
    }


    @Override
    public void run() {

        timer = ActivityMetrics.timer(activityDef, "cycles");

        if (slotState.get() == Finished) {
            logger.warn("Input was already exhausted for slot " + slotId + ", remaining in finished state.");
        }

        slotStateTracker.enterState(Running);
        long cyclenum;
        AtomicLong cycleMax = input.getMax();

        action.init();

        while (slotState.get() == Running) {
            try (Timer.Context cycleTime = timer.time()) {

                cyclenum = input.getAsLong();
                if (cyclenum >= cycleMax.get()) {
                    logger.trace("input exhausted (input " + cyclenum + "), stopping motor thread " + slotId);
                    slotStateTracker.enterState(Finished);
                    continue;
                }

                if (slotState.get() != Running) {
                    logger.trace("motor stopped after input (input " + cyclenum + "), stopping motor thread " + slotId);
                    continue;
                }


                logger.trace("cycle " + cyclenum);
                action.accept(cyclenum);
            }
        }

        //MetricsContext.getInstance().getMetrics().getTimers().get("foo").getMeanRate();
        if (slotState.get() == Stopping) {
            slotStateTracker.enterState(Stopped);
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
        if (slotState.get() == Running) {
            if (input instanceof Stoppable) {
                ((Stoppable) input).requestStop();
            }
            if (action instanceof Stoppable) {
                ((Stoppable) action).requestStop();
            }
            slotStateTracker.enterState(SlotState.Stopping);
        } else {
            logger.warn("attempted to stop motor " + this.getSlotId() + ": from non Running state:" + slotState.get());
        }
    }

}
