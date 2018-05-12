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
package io.engineblock.core;

import io.engineblock.activityapi.core.*;
import io.engineblock.activityapi.input.Input;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.SlotStateTracker;
import io.engineblock.activityimpl.input.ProgressCapable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>An ActivityExecutor is a named instance of an execution harness for a single activity instance.
 * It is responsible for managing threads and activity settings which may be changed while the
 * activity is running.</p>
 *
 * <p>An ActivityExecutor may be represent an activity that is defined and active in the running
 * scenario, but which is inactive. This can occur when an activity is paused by controlling logic,
 * or when the threads are set to zero.</p>
 *
 * <p>
 * Invariants:
 * </p>
 * <ul>
 *     <li>Motors may not receive parameter updates before their owning activities are initialized.</li>
 * </ul>
 */

public class ActivityExecutor implements ActivityController, ParameterMap.Listener, ProgressMeter {
    private static final Logger logger = LoggerFactory.getLogger(ActivityExecutor.class);
    private final List<Motor> motors = new ArrayList<>();
    private final Activity activity;
    private final ActivityDef activityDef;
    private ExecutorService executorService;
    private RuntimeException stoppingException;

    private final static int waitTime=10000;

//    private RunState intendedState = RunState.Uninitialized;

    public ActivityExecutor(Activity activity) {
        this.activity = activity;
        this.activityDef = activity.getActivityDef();
        executorService = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                0L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new IndexedThreadFactory(activity.getAlias(), new ActivityExceptionHandler(this))
        );
        activity.getActivityDef().getParams().addListener(this);
        activity.setActivityController(this);
    }


    // TODO: Doc how uninitialized activities do not propagate parameter map changes and how
    // TODO: this is different from preventing modification to uninitialized activities

    /**
     * <p>True-up the number of motor instances known to the executor. Start all non-running motors.
     * The protocol between the motors and the executor should be safe as long as each state change
     * is owned by either the motor logic or the activity executor but not both, and strictly serialized
     * as well. This is enforced by forcing start(...) to be serialized as well as using CAS on the motor states.</p>
     * <p>The startActivity method may be called to true-up the number of active motors in an activity executor after
     * changes to threads.</p>
     */
    public synchronized void startActivity() {
        logger.info("starting activity " + activity.getAlias() + " for cycles " + activity.getCycleSummary());
        try {
            activity.setRunState(RunState.Starting);
            activity.initActivity();
            activity.onActivityDefUpdate(activityDef);
        } catch (Exception e) {
            this.stoppingException = new RuntimeException("Error initializing activity '" +
                    activity.getAlias() +"': " + e.getMessage(),e);
            logger.error("error initializing activity '" + activity.getAlias() + "': " + stoppingException);
            throw stoppingException;
        }
        adjustToActivityDef(activity.getActivityDef());
        activity.setRunState(RunState.Running);
    }

    /**
     * Simply stop the motors
     */
    public synchronized void stopActivity() {
        activity.setRunState(RunState.Stopped);
        logger.info("stopping activity in progress: " + this.getActivityDef().getAlias());
        motors.forEach(Motor::requestStop);
        motors.forEach(m -> awaitRequiredMotorState(m, 30000, 50, RunState.Stopped, RunState.Finished));
        activity.shutdownActivity();
        activity.closeAutoCloseables();
        logger.info("stopped: " + this.getActivityDef().getAlias() + " with " + motors.size() + " slots");
    }

    /**
     * Shutdown the activity executor, with a grace period for the motor threads.
     *
     * @param initialMillisToWait milliseconds to wait after graceful shutdownActivity request, before forcing everything to stop
     */
    public synchronized void forceStopExecutor(int initialMillisToWait) {
        activity.setRunState(RunState.Stopped);

        executorService.shutdown();
        requestStopMotors();

        try {
            Thread.sleep(initialMillisToWait);
        } catch (InterruptedException ignored) {
        }

        logger.info("stopping activity forcibly " + activity.getAlias());
        List<Runnable> runnables = executorService.shutdownNow();

        activity.shutdownActivity();
        activity.closeAutoCloseables();

        logger.debug(runnables.size() + " threads never started.");

        if (stoppingException!=null) {
            throw stoppingException;
        }
    }

    public boolean requestStopExecutor(int secondsToWait) {
        activity.setRunState(RunState.Stopped);

        logger.info("Stopping executor for " + activity.getAlias() + " when work completes.");

        executorService.shutdown();
        boolean wasStopped = false;
        try {
            wasStopped = executorService.awaitTermination(secondsToWait, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            wasStopped = false;
            logger.warn("while waiting termination of activity " + activity.getAlias() + ", " + ie.getMessage());
        } finally {
            activity.shutdownActivity();
            activity.closeAutoCloseables();
        }
        if (stoppingException!=null) {
            throw stoppingException;
        }

        return wasStopped;
    }


    /**
     * Listens for changes to parameter maps, maps them to the activity instance, and notifies
     * all eligible listeners of changes.
     */
    @Override
    public synchronized void handleParameterMapUpdate(ParameterMap parameterMap) {

        if (activity instanceof ActivityDefObserver) {
            ((ActivityDefObserver)activity).onActivityDefUpdate(activityDef);
        }

        // An activity must be initialized before the motors and other components are
        // considered ready to handle parameter map changes. This is signaled in an activity
        // by the RunState.
        if (activity.getRunState()!=RunState.Uninitialized) {
            if (activity.getRunState()==RunState.Running) {
                adjustToActivityDef(activity.getActivityDef());
            }
            motors.stream()
                    .filter(m -> (m instanceof ActivityDefObserver))
//                    .filter(m -> m.getSlotStateTracker().getSlotState() != RunState.Uninitialized)
//                    .filter(m -> m.getSlotStateTracker().getSlotState() != RunState.Starting)
                    .forEach(m -> ((ActivityDefObserver) m).onActivityDefUpdate(activityDef));
        }
    }

    public ActivityDef getActivityDef() {
        return activityDef;
    }

    public boolean awaitCompletion(int waitTime) {
        return requestStopExecutor(waitTime);
    }

    public boolean awaitFinish(int timeout) {
        boolean awaited = awaitAllRequiredMotorState(timeout, 50, RunState.Finished, RunState.Stopped);
        if (awaited) {
            awaited = awaitCompletion(timeout);
        }
        if (stoppingException!=null) {
            throw stoppingException;
        }
        return awaited;
    }

    public String toString() {
        return getClass().getSimpleName() + "~" + activityDef.getAlias();
    }

    private String getSlotStatus() {
        return motors.stream()
                .map(m -> m.getSlotStateTracker().getSlotState().getCode())
                .collect(Collectors.joining(",", "[", "]"));
    }

    /**
     * Stop extra motors, start missing motors
     *
     * @param activityDef the activityDef for this activity instance
     */
    private synchronized void adjustToActivityDef(ActivityDef activityDef) {
        logger.trace(">-pre-adjust->" + getSlotStatus());

        // Stop and remove extra motor slots
        while (motors.size() > activityDef.getThreads()) {
            Motor motor = motors.get(motors.size() - 1);
            logger.trace("Stopping cycle motor thread:" + motor);
            motor.requestStop();
            motors.remove(motors.size() - 1);
        }

        // Create motor slots
        while (motors.size() < activityDef.getThreads()) {

            Motor motor = activity.getMotorDispenserDelegate().getMotor(activityDef, motors.size());
            logger.trace("Starting cycle motor thread:" + motor);
            motors.add(motor);
        }

        adjustToIntendedActivityState();
        awaitActivityAndMotorStateAlignment();

        logger.trace(">post-adjust->" + getSlotStatus());

    }

    private void adjustToIntendedActivityState() {
        logger.trace("ADJUSTING to INTENDED " + activity.getRunState());
        switch (activity.getRunState()) {
            case Uninitialized:
                break;
            case Starting:
            case Running:
                motors.stream()
                        .filter(m -> m.getSlotStateTracker().getSlotState() != RunState.Running)
                        .filter(m -> m.getSlotStateTracker().getSlotState() != RunState.Finished)
                        .forEach(m -> {
                            m.getSlotStateTracker().enterState(RunState.Starting);
                            executorService.execute(m);
                        });
                break;
            case Stopped:
                motors.stream()
                        .filter(m -> m.getSlotStateTracker().getSlotState() != RunState.Stopped)
                        .forEach(Motor::requestStop);
                break;
            case Finished:
            case Stopping:
                throw new RuntimeException("Invalid requested state in activity executor:" + activity.getRunState());
            default:
                throw new RuntimeException("Unmatched run state:" + activity.getRunState());
        }
    }

    private void awaitActivityAndMotorStateAlignment() {

        switch (activity.getRunState()) {
            case Starting:
            case Running:
                motors.forEach(m -> awaitRequiredMotorState(m, waitTime, 50, RunState.Running, RunState.Finished));
                break;
            case Stopped:
                motors.forEach(m -> awaitRequiredMotorState(m, waitTime, 50, RunState.Stopped, RunState.Finished));
                break;
            case Uninitialized:
                break;
            case Finished:
                motors.forEach(m -> awaitRequiredMotorState(m, waitTime, 50, RunState.Finished));
                break;
            case Stopping:
                throw new RuntimeException("Invalid requested state in activity executor:" + activity.getRunState());
            default:
                throw new RuntimeException("Unmatched run state:" + activity.getRunState());
        }

    }

    /**
     * Await a thread (aka motor/slot) entering a specific SlotState
     *
     * @param m         motor instance
     * @param waitTime  milliseconds to wait, total
     * @param pollTime  polling interval between state checks
     * @param runState any desired SlotState
     * @return true, if the desired SlotState was detected
     */
    private boolean awaitMotorState(Motor m, int waitTime, int pollTime, RunState... runState) {
        long startedAt = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startedAt + waitTime)) {
            for (RunState state : runState) {
                if (m.getSlotStateTracker().getSlotState() == state) {
                    logger.trace(activityDef.getAlias() + "/Motor[" + m.getSlotId() + "] is now in state " + m.getSlotStateTracker().getSlotState());
                    return true;
                }
            }
            try {
                Thread.sleep(pollTime);
            } catch (InterruptedException ignored) {
            }
        }
        logger.trace(activityDef.getAlias() + "/Motor[" + m.getSlotId() + "] is now in state " + m.getSlotStateTracker().getSlotState());
        return false;
    }


    private boolean awaitAllRequiredMotorState(int waitTime, int pollTime, RunState... awaitingState) {
        long startedAt = System.currentTimeMillis();
        boolean awaited = false;
        while (!awaited && (System.currentTimeMillis() < (startedAt + waitTime))) {
            awaited=true;
            for (Motor motor : motors) {
                awaited = awaitMotorState(motor, waitTime, pollTime, awaitingState);
                if (!awaited) {
                    logger.trace("failed awaiting motor " + motor.getSlotId() + " for state in " +
                        Arrays.asList(awaitingState));
                    break;
                }
            }
        }
        return awaited;
    }


    private boolean awaitAnyRequiredMotorState(int waitTime, int pollTime, RunState... awaitingState) {
        long startedAt = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startedAt + waitTime)) {
            for (Motor motor : motors) {
                for (RunState state : awaitingState) {
                    if (motor.getSlotStateTracker().getSlotState() == state) {
                        logger.trace("at least one 'any' of " + activityDef.getAlias() + "/Motor[" + motor.getSlotId() + "] is now in state " + motor.getSlotStateTracker().getSlotState());
                        return true;
                    }
                }
            }
            try {
                Thread.sleep(pollTime);
            } catch (InterruptedException ignored) {
            }
        }
        logger.trace("none of " + activityDef.getAlias() + "/Motor [" + motors.size() + "] is in states in " + Arrays.asList(awaitingState));
        return false;
    }


    /**
     * Await a required thread (aka motor/slot) entering a specific SlotState
     *
     * @param m             motor instance
     * @param waitTime      milliseconds to wait, total
     * @param pollTime      polling interval between state checks
     * @param awaitingState desired SlotState
     * @throws RuntimeException if the waitTime is used up and the desired state is not reached
     */
    private void awaitRequiredMotorState(Motor m, int waitTime, int pollTime, RunState... awaitingState) {
        RunState startingState = m.getSlotStateTracker().getSlotState();
        boolean awaitedRequiredState = awaitMotorState(m, waitTime, pollTime, awaitingState);
        if (!awaitedRequiredState) {
            String error = "Unable to await " + activityDef.getAlias() +
                    "/Motor[" + m.getSlotId() + "]: from state " + startingState + " to " + m.getSlotStateTracker().getSlotState()
                    + " after waiting for " + waitTime +"ms";
            RuntimeException e = new RuntimeException(error);
            logger.error(error);
            throw e;
        }
        logger.trace("motor " + m + " entered awaited state: " + Arrays.asList(awaitingState));
    }

    private synchronized void requestStopMotors() {
        logger.info("stopping activity " + activity);
        activity.setRunState(RunState.Stopped);
        motors.forEach(Motor::requestStop);
    }


    public boolean isRunning() {
        return motors.stream().anyMatch(m -> m.getSlotStateTracker().getSlotState() == RunState.Running);
    }

    public Activity getActivity() {
        return activity;
    }

    @Override
    public synchronized double getProgress() {
        ArrayList<Input> inputs = motors.stream()
                .map(Motor::getInput)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        double startCycle = getActivityDef().getStartCycle();
        double endCycle = getActivityDef().getEndCycle();
        double totalCycles = endCycle - startCycle;

        double total = 0.0D;
        double progress = 0.0D;

        for (Input input : inputs) {
            if (input instanceof ProgressCapable) {
                ProgressCapable progressInput = (ProgressCapable) input;
                total += progressInput.getTotal();
                progress += progressInput.getProgress();

            } else {
                logger.warn("input does not support activity progress: " + input);
                return Double.NaN;
            }
        }

        return progress / total;
    }

    @Override
    public String getProgressName() {
        return activityDef.getAlias();
    }

    @Override
    public RunState getProgressState() {
        Optional<RunState> first = motors.stream()
                .map(Motor::getSlotStateTracker).map(SlotStateTracker::getSlotState)
                .distinct().sorted().findFirst();
        return first.orElse(RunState.Uninitialized);
    }

    public synchronized void notifyException(Thread t, Throwable e) {
        //logger.error("Uncaught exception in activity thread forwarded to activity executor:", e);
        this.stoppingException=new RuntimeException("Error in activity thread " +t.getName(), e);
        forceStopExecutor(10000);
    }

    @Override
    public synchronized void stopActivityWithReason(String reason) {
        logger.info("Stopping activity " + this.activityDef.getAlias() + ": " + reason);
        stopActivity();
    }

    @Override
    public synchronized void stopActivityWithError(Throwable throwable) {
        if (stoppingException==null) {
            this.stoppingException = new RuntimeException(throwable);
            logger.error("stopping on error: " + throwable.toString(), throwable);
        } else {
            if (activityDef.getParams().getOptionalBoolean("fullerrors").orElse(false)) {
                logger.error("additional error: " + throwable.toString(), throwable);
            } else {
                logger.warn("summarized error (fullerrors=false): " + throwable.toString());
            }
        }


    }
}
