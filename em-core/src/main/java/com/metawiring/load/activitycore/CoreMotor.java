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
package com.metawiring.load.activitycore;

import com.metawiring.load.activityapi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>ActivityMotor is a Runnable which runs in one of an activity's many threads.
 * It is the iteration harness for individual cycles of an Activity. Each ActivityMotor
 * instance is responsible for taking input from a LongSupplier and applying
 * the provided LongConsumer to it on each cycle. These two parameters are called
 * input and action, respectively.
 * <p/>
 */
public class CoreMotor implements ActivityDefObserver, Motor {
    private static final Logger logger = LoggerFactory.getLogger(CoreMotor.class);

    private long slotId;
    private final MotorController motorController = new MotorController(this);
    private Input input;
    private Action action;

    /**
     * Create an ActivityMotor.
     *
     * @param slotId The enumeration of the motor, as assigned by its executor.
     * @param input   A LongSupplier which provides the cycle number inputs.
     */
    public CoreMotor(
            long slotId,
            Input input) {
        this.slotId = slotId;
        setInput(input);
    }

    /**
     * Create an ActivityMotor.
     *
     * @param slotId The enumeration of the motor, as assigned by its executor.
     * @param input   A LongSupplier which provides the cycle number inputs.
     * @param action  An LongConsumer which is applied to the input for each cycle.
     */
    public CoreMotor(
            long slotId,
            Input input,
            Action action
    ) {
        this(slotId, input);
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
    public long getSlotId() {
        return this.slotId;
    }

    @Override
    public void requestStop() {

    }

    @Override
    public SlotState getSlotStatus() {
        return motorController.getRunState();
    }

    @Override
    public void run() {
        motorController.signalStarted();
        long cyclenum;
        long cycleMax = input.getMax();

        while (motorController.getRunState() == SlotState.Started) {
            cyclenum = input.getAsLong();
            // TODO: Figure out how to signal any control logic to avoid or react
            // TODO: to graceful motor exits with spurious attempts to restart
            if (cyclenum > cycleMax) {
                logger.trace("input exhausted (input " + cyclenum + "), stopping motor thread " + slotId);
                motorController.requestStop();
                continue;
            }
            logger.trace("cycle " + cyclenum);
            action.accept(cyclenum);
        }

        // TODO:zero-pad activity motor identifiers in log outputs
        motorController.signalStopped();
    }


    @Override
    public String toString() {
        return "slot:" + this.slotId + "; state:" + motorController;
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
}
