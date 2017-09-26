package io.engineblock.core;

import io.engineblock.activityapi.*;
import io.engineblock.activityapi.cycletracking.MarkerDispenser;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.action.CoreActionDispenser;
import io.engineblock.activityimpl.input.CoreInputDispenser;
import io.engineblock.activityimpl.input.TargetRateInput;
import io.engineblock.activityimpl.motor.CoreMotor;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.activityimpl.motor.CoreMotorDispenser;
import io.engineblock.activityimpl.tracker.CoreMarkerDispenser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either exNpress or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
@Test(enabled=true)
public class ActivityExecutorTest {
    private static final Logger logger = LoggerFactory.getLogger(ActivityExecutorTest.class);

    @Test
    public void testDelayedStartSanity() {
        ActivityDef ad = ActivityDef.parseActivityDef("type=diag;alias=test;cycles=10000;initdelay=10000;");
        Optional<ActivityType> activityType = ActivityTypeFinder.instance().get(ad.getActivityType());
        Activity a = new DelayedInitActivity(ad);
        InputDispenser idisp = new CoreInputDispenser(a);
        ActionDispenser adisp = new CoreActionDispenser(a);
        MarkerDispenser tdisp = new CoreMarkerDispenser(a);
        MotorDispenser mdisp = new CoreMotorDispenser(a, idisp, adisp, tdisp);
        a.setActionDispenserDelegate(adisp);
        a.setInputDispenserDelegate(idisp);
        a.setMotorDispenserDelegate(mdisp);

        ActivityExecutor ae = new ActivityExecutor(a);
        ad.setThreads(1);
        ae.startActivity();
        ae.awaitCompletion(15000);
        assertThat(idisp.getInput(10).getPendingCycle()).isEqualTo(10001L);

    }


    @Test(enabled=true)
    public void testNewActivityExecutor() {
        ActivityDef ad = ActivityDef.parseActivityDef("type=diag;alias=test;cycles=1000;");
        Optional<ActivityType> activityType = ActivityTypeFinder.instance().get(ad.getActivityType());
        Input longSupplier = new TargetRateInput(ad);
        MotorDispenser cmf = getActivityMotorFactory(
                ad, motorActionDelay(999), longSupplier
        );
        Activity a = new SimpleActivity(ad);
        InputDispenser idisp = new CoreInputDispenser(a);
        ActionDispenser adisp = new CoreActionDispenser(a);
        MarkerDispenser tdisp = new CoreMarkerDispenser(a);
        MotorDispenser mdisp = new CoreMotorDispenser(a, idisp, adisp, tdisp);
        a.setActionDispenserDelegate(adisp);
        a.setInputDispenserDelegate(idisp);
        a.setMotorDispenserDelegate(mdisp);

        ActivityExecutor ae = new ActivityExecutor(a);
        ad.setThreads(5);
        ae.startActivity();

        int[] speeds = new int[]{1,2000,5,2000,2,2000};
        for(int offset=0; offset<speeds.length; offset+=2) {
            int threadTarget=speeds[offset];
            int threadTime = speeds[offset+1];
            logger.info("Setting thread level to " + threadTarget + " for " +threadTime + " seconds.");
            ad.setThreads(threadTarget);
            try {
                Thread.sleep(threadTime);
            } catch (InterruptedException ignored) {
            }
        }
        ad.setThreads(0);

    }

    private MotorDispenser getActivityMotorFactory(final ActivityDef ad, Action lc, final Input ls) {
        MotorDispenser cmf = new MotorDispenser() {
            @Override
            public Motor getMotor(ActivityDef activityDef, int slotId) {
                Activity activity = new SimpleActivity(activityDef);
                Motor cm = new CoreMotor(activityDef, slotId, ls);
                cm.setAction(lc);
                return cm;
            }
        };
        return cmf;
    }

    private Action motorActionDelay(final long delay) {
        Action consumer = new Action() {
            @Override
            public int runCycle(long value) {
                System.out.println("consuming " + value + ", delaying:" + delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                }
                return 0;
            }
        };
        return consumer;
    }

    private static class DelayedInitActivity extends SimpleActivity {
        private static Logger logger = LoggerFactory.getLogger(DelayedInitActivity.class);

        public DelayedInitActivity(ActivityDef activityDef) {
            super(activityDef);
        }

        @Override
        public void initActivity() {
            Integer initdelay = activityDef.getParams().getOptionalInteger("initdelay").orElse(0);
            logger.info("delaying for " + initdelay);
            try {
                Thread.sleep(initdelay);
            } catch (InterruptedException ignored) {
            }
            logger.info("delayed for " + initdelay);
        }
    }
}