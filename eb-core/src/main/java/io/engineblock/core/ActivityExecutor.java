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

import io.engineblock.activityapi.*;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.SlotStateTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>An ActivityExecutor is a named instance of an execution harness for a single activity instance.
 * It is responsible for managing threads and activity settings which may be changed while the
 * activity is running.</p>
 * <p>An ActivityExecutor may be represent an activity that is defined and active in the running
 * scenario, but which is inactive. This can occur when an activity is paused by controlling logic,
 * or when the threads are set to zero.</p>
 */
public class ActivityExecutor implements ParameterMap.Listener, ProgressMeter {
    private static final Logger logger = LoggerFactory.getLogger(ActivityExecutor.class);
    private final List<Motor> motors = new ArrayList<>();
    private final Activity activity;
    private final ActivityDef activityDef;
    private ExecutorService executorService;
    private SlotState intendedState = SlotState.Initialized;

    public ActivityExecutor(Activity activity) {
        this.activity = activity;
        this.activityDef = activity.getActivityDef();
        executorService = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                0L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new IndexedThreadFactory(activity.getAlias())
        );
        activity.getActivityDef().getParams().addListener(this);
    }

    /**
     * <p>True-up the number of motor instances known to the executor. Start all non-running motors.
     * The protocol between the motors and the executor should be safe as long as each state change
     * is owned by either the motor logic or the activity executor but not both, and strictly serialized
     * as well. This is enforced by forcing start(...) to be serialized as well as using CAS on the motor states.</p>
     * <p>The startActivity method may be called to true-up the number of active motors in an activity executor after
     * changes to threads.</p>
     */
    public synchronized void startActivity() {
        this.intendedState=SlotState.Running;
        logger.info("starting activity " + activity.getAlias() + " for cycles " + activity.getCycleSummary());
        try {
            activity.initActivity();
        } catch (Exception e) {
            logger.error("There was an error starting activity:" + activityDef.getAlias());
            throw new RuntimeException(e);
        }
        adjustToActivityDef(activity.getActivityDef());
    }

    /**
     * Simply stop the motors
     */
    public void stopActivity() {
        this.intendedState=SlotState.Stopped;
        logger.info("stopping activity in progress: " + this.getActivityDef().getAlias());
        motors.forEach(Motor::requestStop);
        motors.forEach(m -> awaitRequiredMotorState(m, 10000, 50, SlotState.Stopped, SlotState.Finished));
        activity.shutdownActivity();
        logger.info("stopped: " + this.getActivityDef().getAlias() + " with " + motors.size() + " slots");
    }

    /**
     * Shutdown the activity executor, with a grace period for the motor threads.
     *
     * @param initialSecondsToWait milliseconds to wait after graceful shutdownActivity request, before forcing everything to stop
     */
    public synchronized void forceStopExecutor(int initialSecondsToWait) {
        this.intendedState=SlotState.Stopped;

        executorService.shutdown();
        requestStopMotors();

        try {
            Thread.sleep(initialSecondsToWait);
        } catch (InterruptedException ignored) {
        }

        logger.info("stopping activity forcibly " + activity.getAlias());
        List<Runnable> runnables = executorService.shutdownNow();

        logger.debug(runnables.size() + " threads never started.");
    }

    public boolean requestStopExecutor(int secondsToWait) {
        intendedState=SlotState.Stopped;

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
        }

        return wasStopped;
    }


    /**
     * Listens for changes to parameter maps, maps them to the activity instance, and notifies
     * all eligible listeners of changes.
     */
    @Override
    public void handleParameterMapUpdate(ParameterMap parameterMap) {
        adjustToActivityDef(activity.getActivityDef());
        motors.stream().filter(
                m -> (m instanceof ActivityDefObserver)
        ).forEach(
                m -> ((ActivityDefObserver) m).onActivityDefUpdate(activityDef)
        );
    }

    public ActivityDef getActivityDef() {
        return activityDef;
    }

    public boolean awaitCompletion(int waitTime) {
        return requestStopExecutor(waitTime);
    }

    public boolean awaitFinish(int timeout) {
        boolean awaited = awaitAllRequiredMotorState(timeout, 50, SlotState.Finished, SlotState.Stopped);
        if (awaited) {
            awaited = awaitCompletion(timeout);
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
            Optional.ofNullable(activity.getActionDispenser()).orElseThrow(() ->
                    new RuntimeException("activityMotorFactory is required"));

            Motor motor = activity.getMotorDispenser().getMotor(activityDef, motors.size());
            logger.trace("Starting cycle motor thread:" + motor);
            motors.add(motor);
        }

        adjustToIntendedActivityState();
        awaitActivityAndMotorStateAlignment();

        logger.trace(">post-adjust->" + getSlotStatus());

    }

    private void adjustToIntendedActivityState() {
        logger.trace("ADJUSTING to INTENDED " + intendedState);
        switch (intendedState) {
            case Starting:
                break;
            case Running:
                motors.stream()
                        .filter(m -> m.getSlotStateTracker().getSlotState() != SlotState.Running)
                        .forEach(m -> {
                            m.getSlotStateTracker().enterState(SlotState.Starting);
                            executorService.execute(m);
                        });
                break;
            case Stopped:
                motors.stream()
                        .filter(m -> m.getSlotStateTracker().getSlotState() != SlotState.Stopped)
                        .forEach(Motor::requestStop);
                break;
            case Initialized:
                break;
            case Finished:
            case Stopping:
                throw new RuntimeException("Invalid requested state in activity executor:" + intendedState);
        }
    }

    private void awaitActivityAndMotorStateAlignment() {

        switch (intendedState) {
            case Running:
                motors.forEach(m -> awaitRequiredMotorState(m, 5000, 50, SlotState.Running, SlotState.Finished));
                break;
            case Stopped:
                motors.forEach(m -> awaitRequiredMotorState(m, 5000, 50, SlotState.Stopped, SlotState.Finished));
                break;
            case Initialized:
                break;
            case Finished:
                motors.forEach(m -> awaitRequiredMotorState(m, 5000, 50, SlotState.Finished));
                break;
            case Stopping:
                throw new RuntimeException("Invalid requested state in activity executor:" + intendedState);
        }

    }

    /**
     * Await a thread (aka motor/slot) entering a specific SlotState
     *
     * @param m         motor instance
     * @param waitTime  milliseconds to wait, total
     * @param pollTime  polling interval between state checks
     * @param slotState any desired SlotState
     * @return true, if the desired SlotState was detected
     */
    private boolean awaitMotorState(Motor m, int waitTime, int pollTime, SlotState... slotState) {
        long startedAt = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startedAt + waitTime)) {
            for (SlotState state : slotState) {
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


    private boolean awaitAllRequiredMotorState(int waitTime, int pollTime, SlotState... awaitingState) {
        long startedAt = System.currentTimeMillis();
        boolean awaited = true;
        while (System.currentTimeMillis() < (startedAt + waitTime)) {
            for (Motor motor : motors) {
                awaited = awaitMotorState(motor, waitTime, pollTime, awaitingState);
                if (!awaited) {
                    logger.trace("failed awaiting motor " + motor.getSlotId() + " for state in " +
                            Arrays.asList(awaitingState));
                    try {
                        Thread.sleep(pollTime);
                    } catch (InterruptedException ignored) {
                    }
                    break;
                }
            }
        }
        return awaited;
    }


    private boolean awaitAnyRequiredMotorState(int waitTime, int pollTime, SlotState... awaitingState) {
        long startedAt = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startedAt + waitTime)) {
            for (Motor motor : motors) {
                for (SlotState state : awaitingState) {
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
    private void awaitRequiredMotorState(Motor m, int waitTime, int pollTime, SlotState... awaitingState) {
        SlotState startingState = m.getSlotStateTracker().getSlotState();
        boolean awaitedRequiredState = awaitMotorState(m, waitTime, pollTime, awaitingState);
        if (!awaitedRequiredState) {
            String error = "Unable to await " + activityDef.getAlias() +
                    "/Motor[" + m.getSlotId() + "]: from state " + startingState + " to " + m.getSlotStateTracker().getSlotState();
            RuntimeException e = new RuntimeException(error);
            logger.error(error);
            throw e;
        }
        logger.trace("motor " + m + " entered awaited state: " + Arrays.asList(awaitingState));
    }

    private synchronized void requestStopMotors() {
        logger.info("stopping activity " + activity);
        intendedState = SlotState.Stopped;
        motors.forEach(Motor::requestStop);
    }


    public boolean isRunning() {
        return motors.stream().anyMatch(m -> m.getSlotStateTracker().getSlotState() == SlotState.Running);
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
        double count = inputs.size();

        double total = 0.0D;

        for (Input input : inputs) {
            double done = (input.getCurrent() - startCycle);
            double fractional = (done / totalCycles);
            total += fractional;
        }

        return total;
    }

    @Override
    public String getProgressName() {
        return activityDef.getAlias();
    }

    @Override
    public SlotState getProgressState() {
        Optional<SlotState> first = motors.stream()
                .map(Motor::getSlotStateTracker).map(SlotStateTracker::getSlotState)
                .distinct().sorted().findFirst();
        return first.orElse(SlotState.Initialized);
    }
}
