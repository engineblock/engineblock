package io.engineblock.core;

import io.engineblock.activityapi.ActivityDef;
import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.Input;
import io.engineblock.activityapi.MotorDispenser;
import io.engineblock.activitycore.CoreInput;
import io.engineblock.activityapi.Motor;
import io.engineblock.activitycore.CoreMotor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

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
@Test(enabled=false)
public class ActivityExecutorTest {
    private static final Logger logger = LoggerFactory.getLogger(ActivityExecutorTest.class);

    @Test(enabled=false)
    public void testNewActivityExecutor() {
        ActivityDef ad = ActivityDef.parseActivityDef("alias=test");
        ActivityExecutor ae = new ActivityExecutor(ad);
        Input longSupplier = new CoreInput(ad);
        MotorDispenser cmf = getActivityMotorFactory(
                ad, motorActionDelay(999), longSupplier
        );
        ae.setActivityMotorDispenser(cmf);
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
                Motor cm = new CoreMotor("activity." + activityDef.getAlias(), slotId, ls);
                cm.setAction(lc);
                return cm;
            }
        };
        return cmf;
    }

    private Action motorActionDelay(final long delay) {
        Action consumer = new Action() {
            @Override
            public void accept(long value) {
                System.out.println("consuming " + value + ", delaying:" + delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                }
            }
        };
        return consumer;
    }
}