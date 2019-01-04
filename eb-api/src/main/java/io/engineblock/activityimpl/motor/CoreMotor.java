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
import io.engineblock.activityapi.core.ops.fluent.opcontext.OpContext;
import io.engineblock.activityapi.core.ops.OpResultBuffer;
import io.engineblock.activityapi.core.ops.fluent.opfacets.OpImpl;
import io.engineblock.activityapi.core.ops.fluent.OpTracker;
import io.engineblock.activityapi.core.ops.fluent.opfacets.TrackedOp;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultSegmentBuffer;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleResultsSegment;
import io.engineblock.activityapi.cyclelog.buffers.results.CycleSegment;
import io.engineblock.activityapi.input.Input;
import io.engineblock.activityapi.output.Output;
import io.engineblock.activityapi.ratelimits.RateLimiter;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.SlotStateTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.engineblock.activityapi.core.RunState.*;

/**
 * ActivityMotor is a Runnable which runs in one of an activity's many threads.
 * It is the iteration harness for individual cycles of an activity. Each ActivityMotor
 * instance is responsible for taking input from a LongSupplier and applying
 * the provided LongConsumer to it on each cycle. These two parameters are called
 * input and action, respectively.
 *
 * This motor implementation splits the handling of sync and async actions with a hard
 * fork in the middle to limit potential breakage of the prior sync implementation
 * with new async logic.
 *
 */
public class CoreMotor<D> implements ActivityDefObserver, Motor, Stoppable {

    private static final Logger logger = LoggerFactory.getLogger(CoreMotor.class);

    private boolean strictmetricnames = true;

    Timer cyclesTimer;
    Timer stridesTimer;
    Timer phasesTimer;

    Timer inputTimer;

    Timer strideWaitTimer;
    Timer strideResponseTimer;
    Timer cycleWaitTimer;
    Timer cycleResponseTimer;

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
    private ArrayDeque<OpContext> contextPool = new ArrayDeque<>();


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
            strideRateLimiter = activity.getStrideLimiter();
            cycleRateLimiter = activity.getCycleLimiter();
            phaseRateLimiter = activity.getPhaseLimiter();

            stridesTimer = activity.getInstrumentation().getOrCreateStridesServiceTimer();
            inputTimer = activity.getInstrumentation().getOrCreateInputTimer();


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

                @SuppressWarnings("unchecked")
                AsyncAction<D> async = AsyncAction.class.cast(action);
                OpTracker<D> tracker = async.getTracker();

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
                        strideDelay = strideRateLimiter.maybeWaitForOp();
                    }

                    StrideTracker strideTracker =
                            new StrideTracker(stridesTimer, cyclesTimer, strideDelay, cycleSegment.peekNextCycle(), stride, output)
                            .start();

                    long strideStart = System.nanoTime();

                    while (!cycleSegment.isExhausted() && slotState.get() == Running) {
                        cyclenum = cycleSegment.nextCycle();
                        if (cyclenum < 0) {
                            if (cycleSegment.isExhausted()) {
                                logger.trace("input exhausted (input " + input + ") via negative read, stopping motor thread " + slotId);
                                slotStateTracker.enterState(Finished);
                                continue;
                            }
                        }

                        if (slotState.get() != Running) {
                            logger.trace("motor stopped in cycle " + cyclenum + ", stopping motor thread " + slotId);
                            continue;
                        }

                        if (cycleRateLimiter != null) {
                            // Block for cycle rate limiter
                            cycleDelay = cycleRateLimiter.maybeWaitForOp();
                        }

                        try {
                            TrackedOp<D> op = new OpImpl<>(tracker);
                            op.setData(async.allocateOpData(cyclenum));
                            op.setWaitTime(cycleDelay);

                            async.enqueue(op);

//                            T opc = async.newOpContext();
//                            opc.addSink(strideTracker);
//                            async.enqueue(opc);
//                            boolean canAcceptMore = async.enqueue(opc);
//                            if (!canAcceptMore) {
//                                logger.trace("Action queue full at cycle=" + cyclenum);
//                            }

                        } catch (Exception t) {
                            logger.error("Error while processing async cycle " + cyclenum + ", error:" + t);
                            throw t;
                        }
                    }


                }

                if (slotState.get() == Finished) {
                    boolean finished = async.awaitCompletion(60000);
                    if (finished) {
                        logger.debug("slot " + this.slotId + " completed successfully");
                    } else {
                        logger.warn("slot " + this.slotId + " was stopped before completing successfully");
                    }
                }

                if (slotState.get() == Stopping) {
                    slotStateTracker.enterState(Stopped);
                }


            } else if (action instanceof SyncAction) {

                cyclesTimer = activity.getInstrumentation().getOrCreateCyclesServiceTimer();
                stridesTimer = activity.getInstrumentation().getOrCreateStridesServiceTimer();
                phasesTimer = activity.getInstrumentation().getOrCreatePhasesServiceTimer();

                if (activity.getActivityDef().getParams().containsKey("async")) {
                    throw new RuntimeException("The async parameter was given for this activity, but it does not seem to know how to do async.");
                }

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
                        strideDelay = strideRateLimiter.maybeWaitForOp();
                    }

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
                                cycleDelay = cycleRateLimiter.maybeWaitForOp();
                            }

                            long cycleStart = System.nanoTime();
                            try {
                                logger.trace("cycle " + cyclenum);

                                // runCycle
                                long phaseStart = System.nanoTime();
                                if (phaseRateLimiter != null) {
                                    phaseDelay = phaseRateLimiter.maybeWaitForOp();
                                }
                                result = sync.runCycle(cyclenum);
                                long phaseEnd = System.nanoTime();
                                phasesTimer.update((phaseEnd - phaseStart) + phaseDelay, TimeUnit.NANOSECONDS);

                                // ... runPhase ...
                                if (multiPhaseAction != null) {
                                    while (multiPhaseAction.incomplete()) {
                                        phaseStart = System.nanoTime();
                                        if (phaseRateLimiter != null) {
                                            phaseDelay = phaseRateLimiter.maybeWaitForOp();
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

        this.strictmetricnames = activityDef.getParams().getOptionalBoolean("strictmetricnames").orElse(true);
        for (Object component : (new Object[]{input, action, output})) {
            if (component instanceof ActivityDefObserver) {
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


//    @Override
//    public void onAfterOpStop(OpContext opc) {
//        cyclesTimer.update(opc.getFinalResponseTime(), TimeUnit.NANOSECONDS);
//        if (output != null) {
//            try {
//                output.onCycleResult(opc);
//            } catch (Exception t) {
//            }
//        }
//
//    }

    public static class StrideTracker extends OpResultBuffer {

        private final Timer strideTimer;
        private final Timer cycleTimer;
        private final Output output;

        public StrideTracker(Timer strideTimer, Timer cycleTimer, long strideDelay, long initialCycle, int size, Output output) {
            super(initialCycle, strideDelay, OpContext[].class, size);
            this.strideTimer = strideTimer;
            this.cycleTimer = cycleTimer;
            this.output = output;
        }

        /**
         * Each stride tracker must be started before any ops that it tracks
         * @return the stride tracker, for method chaining
         */
        public StrideTracker start() {
            this.getContext().start();
            return this;
        }

        /**
         * When an op that is tracked by this stride tracker completes, this method must be called
         * @param opc The op context to be updated in the stride tracker
         */
        @Override
        public void onAfterOpStop(OpContext opc) {
            cycleTimer.update(opc.getFinalResponseTime(), TimeUnit.NANOSECONDS);
            super.onAfterOpStop(opc);
        }

        /**
         * When a stride is complete, do house keeping. This effectively means when N==stride ops have been
         * submitted to this buffer, which is tracked by {@link OpResultBuffer#put(Object)}.
         */
        public void onFull() {
            getContext().stop(0);
            strideTimer.update(this.getContext().getFinalResponseTime(),TimeUnit.NANOSECONDS);
            logger.trace("completed stride with first result cycle (" + getContext().getCycle() + ")");

            if (output != null) {
                try {
                    flip();
                    int remaining = remaining();
                    for (int i = 0; i < remaining; i++) {
                        OpContext opc = get();
                        output.onCycleResult(opc);
                    }
                } catch (Exception t) {
                    logger.error("Error while feeding cycle result to output '" + output + "', error:" + t);
                    throw t;
                }
            }
        }


    }
}
