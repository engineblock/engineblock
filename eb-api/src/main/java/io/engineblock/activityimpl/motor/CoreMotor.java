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
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResult;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultSegmentBuffer;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultsSegment;
import io.engineblock.activityapi.input.Input;
import io.engineblock.activityapi.output.Output;
import io.engineblock.activityapi.rates.RateLimiter;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.SlotStateTracker;
import io.engineblock.metrics.ActivityMetrics;
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
public class CoreMotor implements ActivityDefObserver, Motor, Stoppable, OpResultBuffer.Sink<OpContext> {

    private static final Logger logger = LoggerFactory.getLogger(CoreMotor.class);
    Timer cyclesTimer;
    Timer phasesTimer;
    Timer stridesTimer;
    Timer inputTimer;
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
            cyclesTimer = ActivityMetrics.timer(activity.getActivityDef(), "cycles");
            phasesTimer = ActivityMetrics.timer(activity.getActivityDef(), "phases");
            stridesTimer = ActivityMetrics.timer(activity.getActivityDef(), "strides");
            inputTimer = ActivityMetrics.timer(activity.getActivityDef(), "read_input");

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


            long strideDelay = 0L;
            long cycleDelay = 0L;
            long phaseDelay = 0L;

            // Reviewer Note: This separate of code paths was used to avoid impacting the
            // previously logic for the SyncAction type. It may be consolidated later once
            // the async action is proven durable
            if (action instanceof AsyncAction) {
                AsyncAction async = (AsyncAction) action;

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

                    if (strideRateLimiter != null) {
                        // block for strides rate limiter
                        strideDelay = strideRateLimiter.acquire();
                    }

                    StrideResultBuffer strideResultBuffer = new StrideResultBuffer(cyclesTimer, strideDelay, cycleSegment.peekNextCycle(), this, stride);


//                try (Timer.Context stridesTime = stridesTimer.time()) {
                    long strideStart = System.nanoTime();

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

                        if (cycleRateLimiter != null) {
                            // Block for cycle rate limiter
                            cycleDelay = cycleRateLimiter.acquire();
                        }

                        //try (Timer.Context cycleTime = cyclesTimer.time()) {
                        try {
                            if (!async.enqueue(new OpContext(strideResultBuffer, cyclenum, cycleDelay))) {
                                logger.trace("Action queue full at cycle=" + cyclenum);

                                OpContext opContext = async.dequeue();
                                cyclesTimer.update(opContext.getTotalLatency(), TimeUnit.NANOSECONDS);
                            }

//                                // runCycle
//                                long phaseStart = System.nanoTime();
//                                if (phaseRateLimiter != null) {
//                                    phaseDelay = phaseRateLimiter.acquire();
//                                }
//                                long phaseEnd = System.nanoTime();
//                                phasesTimer.update((phaseEnd - phaseStart) + phaseDelay, TimeUnit.NANOSECONDS);

//                                // ... runPhase ...
//                                if (multiPhaseAction != null) {
//                                    while (multiPhaseAction.incomplete()) {
//                                        phaseStart = System.nanoTime();
//                                        if (phaseRateLimiter != null) {
//                                            phaseDelay = phaseRateLimiter.acquire();
//                                        }
//                                        result = multiPhaseAction.runPhase(cyclenum);
//                                        phaseEnd = System.nanoTime();
//                                        phasesTimer.update((phaseEnd - phaseStart) + phaseDelay, TimeUnit.NANOSECONDS);
//                                    }
//                                }

                        } catch (Exception t) {
                            logger.error("Error while processing async cycle " + cyclenum + ", error:" + t);
                            throw t;
                        }
                    }


                }

                CycleResult result = async.dequeue();
                while (result != null) {
                    if (output != null) {
                        try {
                            output.onCycleResult(result);
                        } catch (Exception t) {
                            logger.error("Error while feeding cycle result  " + result + " to output '" + output + "', error:" + t);
                            throw t;
                        }
                    }
                    result = async.dequeue();
                }

                if (slotState.get() == Stopping) {
                    slotStateTracker.enterState(Stopped);
                }


            } else if (action instanceof SyncAction) {
                SyncAction sync = (SyncAction) action;

                while (slotState.get() == Running) {

                    CycleSegment cycleSegment = null;
                    CycleResultSegmentBuffer segBuffer = new CycleResultSegmentBuffer(stride);

                    try (Timer.Context inputTime = inputTimer.time()) {
                        cycleSegment = input.getInputSegment(stride);
                    }

                    if (cycleSegment == null) {
                        logger.debug("input exhausted (input " + input + ") via null segment, stopping motor thread " + slotId);
                        slotStateTracker.enterState(Finished);
                        continue;
                    }


                    if (strideRateLimiter != null) {
                        // block for strides rate limiter
                        strideDelay = strideRateLimiter.acquire();
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
                                cycleDelay = cycleRateLimiter.acquire();
                            }

                            //try (Timer.Context cycleTime = cyclesTimer.time()) {
                            long cycleStart = System.nanoTime();
                            try {
                                logger.trace("cycle " + cyclenum);

                                // runCycle
                                long phaseStart = System.nanoTime();
                                if (phaseRateLimiter != null) {
                                    phaseDelay = phaseRateLimiter.acquire();
                                }
                                result = sync.runCycle(cyclenum);
                                long phaseEnd = System.nanoTime();
                                phasesTimer.update((phaseEnd - phaseStart) + phaseDelay, TimeUnit.NANOSECONDS);

                                // ... runPhase ...
                                if (multiPhaseAction != null) {
                                    while (multiPhaseAction.incomplete()) {
                                        phaseStart = System.nanoTime();
                                        if (phaseRateLimiter != null) {
                                            phaseDelay = phaseRateLimiter.acquire();
                                        }
                                        result = multiPhaseAction.runPhase(cyclenum);
                                        phaseEnd = System.nanoTime();
                                        phasesTimer.update((phaseEnd - phaseStart) + phaseDelay, TimeUnit.NANOSECONDS);
                                    }
                                }

                            } finally {
                                long cycleEnd = System.nanoTime();
                                cyclesTimer.update((cycleEnd - cycleStart) + cycleDelay, TimeUnit.NANOSECONDS);
                            }
                            segBuffer.append(cyclenum, result);
                        }

                    } finally {
                        long strideEnd = System.nanoTime();
                        stridesTimer.update((strideEnd - strideStart) + strideDelay, TimeUnit.NANOSECONDS);
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

            } else {
                throw new RuntimeException("Valid Action implementations must implement either the SyncAction or the AsyncAction sub-interface");
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

    @Override
    public void handle(OpResultBuffer<OpContext> strideResults) {
        OpContext strideOps = strideResults.get();
        strideOps.stop();
        stridesTimer.update(strideOps.getTotalLatency(), TimeUnit.NANOSECONDS);
        logger.trace("completed stride with first result cycle (" + strideOps.getCycle() + ")");
        if (output != null) {
            int remaining = strideResults.remaining();
            for (int i = 0; i < remaining; i++) {
                OpContext opContext = strideResults.get();
                output.onCycleResult(opContext.getCycle(), opContext.getResult());
            }
        }
    }

    public static class StrideResultBuffer extends OpResultBuffer<OpContext> {

        private final Timer cycleTimer;

        public StrideResultBuffer(Timer cycleTimer, long strideDelay, long initialCycle, OpResultBuffer.Sink<OpContext> sink, int size) {
            super(new OpContext(null, initialCycle, strideDelay), sink, OpContext[].class, size);
            this.cycleTimer = cycleTimer;
        }

        @Override
        public void handle(OpContext opContext) {
            cycleTimer.update(opContext.getTotalLatency(), TimeUnit.NANOSECONDS);
            super.handle(opContext);
        }
    }
}
