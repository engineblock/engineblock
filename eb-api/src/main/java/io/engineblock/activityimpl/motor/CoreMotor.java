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
import io.engineblock.activityapi.core.*;
import io.engineblock.activityapi.cyclelog.buffers.cycles.CycleSegment;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultSegmentBuffer;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultsSegment;
import io.engineblock.activityapi.input.Input;
import io.engineblock.activityapi.output.Output;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.SlotStateTracker;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.activityapi.rates.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.engineblock.activityapi.core.RunState.*;

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
    private Activity activity;
    private SlotStateTracker slotStateTracker;
    private AtomicReference<RunState> slotState;
    private int stride = 1;
    private Output output;
    private RateLimiter strideRateLimiter;
    private RateLimiter cycleRateLimiter;
    private RateLimiter phaseRateLimiter;

    /**
     * Create an ActivityMotor.
     *
     * @param activity The activity that this motor will be associated with.
     * @param slotId   The enumeration of the motor, as assigned by its executor.
     * @param input    A LongSupplier which provides the cycle number inputs.
     */
    public CoreMotor(
            Activity activity,
            long slotId,
            Input input) {
        this.activity = activity;
        this.slotId = slotId;
        setInput(input);
        slotStateTracker = new SlotStateTracker(slotId);
        slotState = slotStateTracker.getAtomicSlotState();
        onActivityDefUpdate(activity.getActivityDef());
    }


    /**
     * Create an ActivityMotor.
     *
     * @param activity The activity that this motor is based on.
     * @param slotId   The enumeration of the motor, as assigned by its executor.
     * @param input    A LongSupplier which provides the cycle number inputs.
     * @param action   An LongConsumer which is applied to the input for each cycle.
     */
    public CoreMotor(
            Activity activity,
            long slotId,
            Input input,
            Action action
    ) {
        this(activity, slotId, input);
        setAction(action);
    }

    /**
     * Create an ActivityMotor.
     *
     * @param activity The activity that this motor is based on.
     * @param slotId   The enumeration of the motor, as assigned by its executor.
     * @param input    A LongSupplier which provides the cycle number inputs.
     * @param action   An LongConsumer which is applied to the input for each cycle.
     * @param output   An optional tracker.
     */
    public CoreMotor(
            Activity activity,
            long slotId,
            Input input,
            Action action,
            Output output
    ) {
        this(activity, slotId, input);
        setAction(action);
        setResultOutput(output);
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

        try {
            Timer cyclesTimer = ActivityMetrics.timer(activity.getActivityDef(), "cycles");
            Timer phasesTimer = ActivityMetrics.timer(activity.getActivityDef(), "phases");
            Timer stridesTimer = ActivityMetrics.timer(activity.getActivityDef(), "strides");
            Timer inputTimer = ActivityMetrics.timer(activity.getActivityDef(), "read-input");

            strideRateLimiter = activity.getStrideLimiter();
            cycleRateLimiter = activity.getCycleLimiter();
            phaseRateLimiter = activity.getPhaseLimiter();

            if (slotState.get() == Finished) {
                logger.warn("Input was already exhausted for slot " + slotId + ", remaining in finished state.");
            }

            slotStateTracker.enterState(Running);

            MultiPhaseAction multiPhaseAction = null;
            if (action instanceof MultiPhaseAction) {
                multiPhaseAction = ((MultiPhaseAction) action);
            }

            long cyclenum;
            action.init();

            if (input instanceof Startable) {
                ((Startable) input).start();
            }

            if (strideRateLimiter != null) {
                // block for strides rate limiter
                strideRateLimiter.start();
            }


            long strideDelay=0L;
            long cycleDelay=0L;
            long phaseDelay=0L;
            while (slotState.get() == Running) {

                CycleSegment cycleSegment = null;

                try (Timer.Context inputTime = inputTimer.time()) {
                    cycleSegment = input.getInputSegment(stride);
                }

                if (cycleSegment == null) {
                    logger.debug("input exhausted (input " + input + ") via null segment, stopping motor thread " + slotId);
                    slotStateTracker.enterState(Finished);
                    continue;
                }

                CycleResultSegmentBuffer segBuffer = new CycleResultSegmentBuffer(stride);

                if (strideRateLimiter != null) {
                    // block for strides rate limiter
                    strideDelay=strideRateLimiter.acquire();
                }

//                try (Timer.Context stridesTime = stridesTimer.time()) {
                long strideStart = System.nanoTime();
                try {

                    while (!cycleSegment.isExhausted()) {
                        cyclenum = cycleSegment.nextCycle();
                        if (cyclenum < 0) {
                            if (cycleSegment.isExhausted()) {
                                logger.trace("input exhausted (input " + input + ") via negative read, stopping motor thread " + slotId);
                                slotStateTracker.enterState(Finished);
                                continue;
                            }
                        }

                        if (slotState.get() != Running) {
                            logger.trace("motor stopped after input (input " + cyclenum + "), stopping motor thread " + slotId);
                            continue;
                        }
                        int result = -1;

                        if (cycleRateLimiter != null) {
                            // Block for cycle rate limiter
                            cycleDelay=cycleRateLimiter.acquire();
                        }

                        // Phases are rate limited independently from overall cycles, but each cycle has at least one phase.
                        if (phaseRateLimiter != null) {
                            // Block for cycle rate limiter
                            phaseDelay=phaseRateLimiter.acquire();
                        }

                        //try (Timer.Context cycleTime = cyclesTimer.time()) {
                        long cycleStart=System.nanoTime();
                        try {

                            logger.trace("cycle " + cyclenum);
                            try (Timer.Context phaseTime = phasesTimer.time()) {
                                result = action.runCycle(cyclenum);
                            }

                            if (multiPhaseAction != null) {
                                while (multiPhaseAction.incomplete()) {
                                    if (phaseRateLimiter != null) {
                                        // Block for cycle rate limiter
                                        phaseDelay=phaseRateLimiter.acquire();
                                    }
                                    try (Timer.Context phaseTime = phasesTimer.time()) {
                                        result = multiPhaseAction.runPhase(cyclenum);
                                    }
                                }
                            }

                        } finally {
                            long cycleEnd=System.nanoTime();
                            cyclesTimer.update((cycleEnd-cycleStart)+cycleDelay,TimeUnit.NANOSECONDS);
                        }
                        segBuffer.append(cyclenum, result);
                    }

                } finally {
                    long strideEnd=System.nanoTime();
                    stridesTimer.update((strideEnd-strideStart)+strideDelay, TimeUnit.NANOSECONDS);
                }

                if (output != null) {
                    CycleResultsSegment outputBuffer = segBuffer.toReader();
                    try {
                        output.onCycleResultSegment(outputBuffer);
                    } catch (Exception t) {
                        logger.error("Error while feeding result segment " + outputBuffer + " to output '" + output + "', error:" + t);
                        throw t;
                    }
                }
            }

            if (slotState.get() == Stopping) {
                slotStateTracker.enterState(Stopped);
            }
        } catch (Throwable t) {
            logger.error("Error in core motor loop:" + t, t);
            throw t;
        }
    }


    @Override
    public String toString() {
        return "slot:" + this.slotId + "; state:" + slotState.get();
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        for (Object component : (new Object[]{input, action, output})) {
            if (component != null && component instanceof ActivityDefObserver) {
                ((ActivityDefObserver) component).onActivityDefUpdate(activityDef);
            }
        }

        this.stride = activityDef.getParams().getOptionalInteger("stride").orElse(1);
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
            slotStateTracker.enterState(RunState.Stopping);
        } else {
            if (slotState.get() != Stopped && slotState.get() != Stopping) {
                logger.warn("attempted to stop motor " + this.getSlotId() + ": from non Running state:" + slotState.get());
            }
        }
    }

    public void setResultOutput(Output resultOutput) {
        this.output = resultOutput;
    }

}
