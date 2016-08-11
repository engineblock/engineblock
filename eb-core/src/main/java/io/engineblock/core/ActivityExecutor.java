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
 * <p>
 * <p>An ActivityExecutor may be represent an activity that is defined and active in the running
 * scenario, but which is inactive. This can occur when an activity is paused by controlling logic,
 * or when the threads are set to zero.</p>
 */
public class ActivityExecutor implements ParameterMap.Listener {
    private static final Logger logger = LoggerFactory.getLogger(ActivityExecutor.class);
    private final List<Motor> motors = new ArrayList<>();
    private final Activity activity;
    private final ActivityDef activityDef;
    //    private ActivityDef activityDef;
    private ExecutorService executorService;
//    private MotorDispenser activityMotorDispenser;
    private SlotState intendedState = SlotState.Initialized;

    public ActivityExecutor(Activity activity) {
        this.activity = activity;
        this.activityDef = activity.getActivityDef();
//        this.activityMotorDispenser = activity.getMotorDispenser();
        executorService = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                0L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new IndexedThreadFactory(activity.getAlias())
        );
//        ScopedCachingGeneratorSource gi = new ScopedGeneratorCache(
//                new GeneratorInstantiator(), RuntimeScope.activity
//        );
        activity.getActivityDef().getParams().addListener(this);
    }

//    public void setActivityMotorDispenser(MotorDispenser activityMotorDispenser) {
//        this.activityMotorDispenser = activityMotorDispenser;
//    }
//
    /**
     * <p>True-up the number of motor instances known to the executor. Start all non-running motors.
     * The protocol between the motors and the executor should be safe as long as each state change
     * is owned by either the motor logic or the activity executor but not both, and strictly serialized
     * as well. This is enforced by forcing start(...) to be serialized as well as using CAS on the motor states.</p>
     * <p>The startActivity method may be called to true-up the number of active motors in an activity executor after
     * changes to threads.</p>
     */
    public synchronized void startActivity() {
        logger.info("starting activity " + activity.getAlias());
        activity.initActivity();
        this.intendedState=SlotState.Started;
        adjustToActivityDef(activity.getActivityDef());
    }

    /**
     * Simply stop the motors
     */
    public void stopActivity() {
        logger.info("stopping activity in progress: " + this.getActivityDef().getAlias());
        this.intendedState=SlotState.Stopped;
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
        logger.info("Stopping executor for " + activity.getAlias());
        intendedState=SlotState.Stopped;

        executorService.shutdown();
        boolean wasStopped = false;
        try {
            wasStopped = executorService.awaitTermination(secondsToWait, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            wasStopped = false;
        }

        // TODO: This is a dirty hack and it has to be fixed
        motors.stream().findAny().ifPresent(m -> {
            if (m.getAction() instanceof ActivityShutdown) {
                logger.info("Calling shutdownActivity on activity " + activity + "with slot:" + m.getSlotId());
                ((ActivityShutdown) m.getAction()).shutdownActivity();
            }
        });

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
                .map(m -> m.getSlotState().getCode())
                .collect(Collectors.joining(",", "[", "]"));
    }

    /**
     * Stop extra motors, start missing motors
     *
     * @param activityDef the activityDef for this activity instance
     */
    private synchronized void adjustToActivityDef(ActivityDef activityDef) {
        logger.trace(">-pre-adjust->" + getSlotStatus());

        adjustToIntendedState();

        // Stop and remove extra motor slots
        while (motors.size() > activityDef.getThreads()) {
            Motor motor = motors.get(motors.size() - 1);
            logger.trace("Stopping cycle motor thread:" + motor);
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

        adjustToIntendedState();

        // Await all motors to be in started or finished state
        motors.forEach(m -> awaitRequiredMotorState(m, 5000, 50, SlotState.Started, SlotState.Finished));

        logger.trace(">post-adjust->" + getSlotStatus());

    }

    private void adjustToIntendedState() {
        switch (intendedState) {
            case Started:
                motors.stream()
                        .filter(m -> m.getSlotState() != SlotState.Started)
                        .forEach(executorService::execute);
                break;
            case Stopped:
                motors.stream()
                        .filter(m -> m.getSlotState() != SlotState.Stopped)
                        .forEach(Motor::requestStop);
                break;
            case Initialized:
            case Finished:
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
                if (m.getSlotState() == state) {
                    logger.trace(activityDef.getAlias() + "/Motor[" + m.getSlotId() + "] is now in state " + m.getSlotState());
                    return true;
                }
            }
            try {
                Thread.sleep(pollTime);
            } catch (InterruptedException ignored) {
            }
        }
        logger.trace(activityDef.getAlias() + "/Motor[" + m.getSlotId() + "] is now in state " + m.getSlotState());
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
                    if (motor.getSlotState() == state) {
                        logger.trace("at least one 'any' of " + activityDef.getAlias() + "/Motor[" + motor.getSlotId() + "] is now in state " + motor.getSlotState());
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
        SlotState startingState = m.getSlotState();
        boolean awaitedRequiredState = awaitMotorState(m, waitTime, pollTime, awaitingState);
        if (!awaitedRequiredState) {
            String error = "Unable to await " + activityDef.getAlias() +
                    "/Motor[" + m.getSlotId() + "]: from state " + startingState + " to " + m.getSlotState();
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
        return motors.stream().anyMatch(m -> m.getSlotState() == SlotState.Started);
    }
}
