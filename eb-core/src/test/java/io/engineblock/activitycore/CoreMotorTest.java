package io.engineblock.activitycore;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.Motor;
import io.engineblock.activitycore.fortesting.BlockingCycleValueSupplier;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

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
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
@Test(enabled=false)
public class CoreMotorTest {

    @Test(enabled=false)
    public void testBasicActivityMotor() {
        BlockingCycleValueSupplier lockstepper = new BlockingCycleValueSupplier();
        Motor cm = new CoreMotor("testing-basic-activity-motor", 5L, lockstepper);
        AtomicLong observableAction = new AtomicLong(-3L);
        cm.setAction(getTestConsumer(observableAction));
        Thread t = new Thread(cm);
        t.setName("TestMotor");
        t.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        lockstepper.setForSingleReader(5L);
        boolean result = awaitCondition(atomicInteger -> (atomicInteger.get()==5L),observableAction,5000,100);
        assertThat(observableAction.get()).isEqualTo(5L);
    }

    private Action getTestConsumer(final AtomicLong atomicLong) {
        return new Action() {
            @Override
            public void accept(long value) {
                atomicLong.set(value);
            }
        };
    }
    private boolean awaitCondition(Predicate<AtomicLong> atomicPredicate, AtomicLong atomicInteger, long millis, long retry) {
        long start = System.currentTimeMillis();
        long now=start;
        while (now < start + millis) {
            boolean result = atomicPredicate.test(atomicInteger);
            if (result) {
                return true;
            } else {
                try {
                    Thread.sleep(retry);
                } catch (InterruptedException ignored) {}
            }
            now = System.currentTimeMillis();
        }
        return false;
    }
}