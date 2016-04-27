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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
 */
public class ActivityExecutor implements ParameterMap.Listener {
    private static final Logger logger = LoggerFactory.getLogger(ActivityExecutor.class);

    private ActivityDef activityDef;

    private ExecutorService executorService;
    private MotorDispenser activityMotorDispenser;
    private final List<Motor> motors = new ArrayList<>();

    public ActivityExecutor(ActivityDef activityDef) {
        this.activityDef = activityDef;
        executorService = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                0L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new IndexedThreadFactory(activityDef.getAlias())
        );
//        ScopedCachingGeneratorSource gi = new ScopedGeneratorCache(
//                new GeneratorInstantiator(), RuntimeScope.activity
//        );
        activityDef.getParams().addListener(this);
    }

    public void setActivityMotorDispenser(MotorDispenser activityMotorDispenser) {
        this.activityMotorDispenser = activityMotorDispenser;
    }

    /**
     * <p>True-up the number of motor instances known to the executor. Start all non-running motors.
     * The protocol between the motors and the executor should be safe as long as each state change
     * is owned by either the motor logic or the activity executor but not both, and strictly serialized
     * as well. This is enforced by forcing start() to be serialized as well as using CAS on the motor states.</p>
     *
     * <p>The start method may be called to true-up the number of active motors in an activity executor after
     * changes to threads.</p>
     */
    public synchronized void start() {
        logger.info("starting activity " + activityDef.getLogName());
        adjustToActivityDef(activityDef);

    }

    private String slotStatus() {
        return motors.stream()
                .map(m -> m.getSlotStatus().getCode())
                .collect(Collectors.joining(",", "[", "]"));
    }

    private synchronized void adjustToActivityDef(ActivityDef activityDef) {
        logger.debug(">-pre-adjust->" + slotStatus());

        Optional.ofNullable(activityMotorDispenser).orElseThrow(() ->
                new RuntimeException("activityMotorFactory is required"));

        while (motors.size() > activityDef.getThreads()) {
            Motor motor = motors.get(motors.size() - 1);
            logger.trace("Stopping cycle motor thread:" + motor);
            motor.requestStop();
            motors.remove(motors.size() - 1);
        }

        while (motors.size() < activityDef.getThreads()) {
            Motor motor = activityMotorDispenser.getMotor(activityDef, motors.size());
            logger.trace("Starting cycle motor thread:" + motor);
            motors.add(motor);
        }

        motors.stream()
                .filter(m -> m.getSlotStatus() != SlotState.Started)
                .forEach(executorService::execute);

        motors.stream()
                .forEach(m -> awaitStartup(m, 1000));

        logger.debug(">post-adjust->" + slotStatus());

    }

    private void awaitStartup(Motor m, int i) {
        long startedAt = System.currentTimeMillis();
        while ((m.getSlotStatus()!=SlotState.Started) && System.currentTimeMillis() < (startedAt + i)) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }
        if ((m.getSlotStatus()!=SlotState.Started)) {
            throw new RuntimeException("thread startup delayed excessivly. Adjust timeout or investigate.");
        }
        logger.trace(activityDef.getAlias() + "/Motor[" + m.getSlotId() + "] is now running.");
    }

    public synchronized void forceStop() {
        stop();
        try {
            Thread.sleep(1000); // Well, yeah, but this isn't that complicated, people.
        } catch (InterruptedException ignored) {
        }

        logger.info("stopping activity forcibly " + activityDef.getLogName());
        List<Runnable> runnables = executorService.shutdownNow();

        logger.debug(runnables.size() + " threads never started.");

    }

    public synchronized void stop() {
        logger.info("stopping activity " + activityDef.getLogName());
        for (Motor motor : motors) {
            Optional.of(motor)
                    .ifPresent(Motor::requestStop);
//                    .map(ActivityMotor::getMotorController)
//                    .ifPresent(MotorController::requestStop);
        }
    }

    public void stopExecutor() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleParameterMapUpdate(ParameterMap parameterMap) {
        adjustToActivityDef(activityDef);
        motors.stream().filter(
                m -> (m instanceof ActivityDefObserver)
        ).forEach(
                m -> ((ActivityDefObserver) m).onActivityDefUpdate(activityDef)
        );
        // TODO: clean this up: activityDef or parameterMap? Pick one.
    }

    public ActivityDef getActivityDef() {
        return activityDef;
    }

    public String toString() {
        return getClass().getSimpleName() + "~" + activityDef.getAlias();
    }
}
