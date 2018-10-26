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

package io.engineblock.activityapi.rates;

import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests run all the rate limiter micro benches with average rate
 * limiting only, due to the burstRatio level being set to 0.0D.
 */
@Test
public class TestRateLimiterLogic {


    /**
     * In this scenario, ops attempt to start faster than real-time allowance after an
     * initial delay of 1000ns and no bursting allowed.
     * This means that strict timing requires that the lost time will never be given
     * back, and the initial wait time will be 1000ns at minimum.
     */
    @Test
    public void testReportedStrictDelay() {
        AtomicLong clock = new AtomicLong(50_000);
        TestableDynamicRateLimiter rl =
                new TestableDynamicRateLimiter(clock,new RateSpec("1000,1.0,dynamic"),ActivityDef.parseActivityDef("alias=testing"));
        //rl.start;
        clock.set(clock.get() + 1000);
        long delay0 = rl.acquire();
        assertThat(delay0).isEqualTo(1000L);
        long delay1 = rl.acquire();
        assertThat(delay1).isEqualTo(1000L);
        long delay2 = rl.acquire();
        assertThat(delay2).isEqualTo(1000L);
    }

    /**
     * In this scenario, ops attempt to start faster than real-time allowance after an
     * initial delay of 1000ns with a minor amount of bursting allowed.
     */
    @Test
    public void testReportedBurstDelay() {
        AtomicLong clock = new AtomicLong(50_000);
        TestableDynamicRateLimiter rl =
                new TestableDynamicRateLimiter(clock,new RateSpec("1000,1.5,dynamic"),ActivityDef.parseActivityDef("alias=testing"));
        //rl.start;
        clock.set(clock.get() + 1000000);
        long delay0 = rl.acquire();
        assertThat(delay0).isEqualTo(1000000L);
        long delay1 = rl.acquire();
        assertThat(delay1).isEqualTo(666666L);
        long delay2 = rl.acquire();
        assertThat(delay2).isEqualTo(333332L);
        long delay3 = rl.acquire();
        assertThat(delay3).isEqualTo(0L);
        long delay4 = rl.acquire();
        assertThat(delay4).isEqualTo(0L);
        long delay5 = rl.acquire();
        assertThat(delay5).isEqualTo(0L);
    }

    @Test
    public void testDelayCalculations() {
        AtomicLong clock = new AtomicLong(50_000);
        TestableDynamicRateLimiter rl = new TestableDynamicRateLimiter(
                clock, new RateSpec(1000D, 0.0), ActivityDef.parseActivityDef("alias=testing")
        );
        //rl.start;
//        assertThat(rl.getLastSeenNanoTimeline()).isEqualTo(50_000L);
        assertThat(rl.getAllocatedNanos().get()).isEqualTo(50_000L);
        assertThat(rl.getWaitTime()).isEqualTo(0L);
        assertThat(rl.getTotalWaitTime()).isEqualTo(0L);

        clock.set(clock.get() + 50_000L);
        assertThat(rl.getWaitTime()).isEqualTo(50_000L);
        assertThat(rl.getTotalWaitTime()).isEqualTo(50_000L);

        assertThat(rl.getAllocatedNanos().addAndGet(25_000L)).isEqualTo(75000L);
        assertThat(rl.getWaitTime()).isEqualTo(25_000L);
        assertThat(rl.getTotalWaitTime()).isEqualTo(25_000);

         long delay = rl.acquire();
        assertThat(delay).isEqualTo(25000L);

//        assertThat(rl.getLastSeenNanoTimeline()).isEqualTo(100_000L);
        assertThat(rl.getAllocatedNanos().get()).isEqualTo(1075000L);
        assertThat(rl.getWaitTime()).isEqualTo(0L); // accurate for testable, but not allowed for the main impl
        assertThat(rl.getTotalWaitTime()).isEqualTo(0L);

        clock.set(clock.get() + 100_000L);
        rl.setRateSpec(rl.getRateSpec().withOpsPerSecond(2000));
//        assertThat(rl.getLastSeenNanoTimeline()).isEqualTo(200_000L);
        rl.acquire();
        assertThat(rl.getAllocatedNanos().get()).isEqualTo(1575000L);
        assertThat(rl.getWaitTime()).isEqualTo(0L);
        assertThat(rl.getTotalWaitTime()).isEqualTo(0L);

        clock.set(clock.get()+60_000_000_000L);
        delay = rl.acquire();
        assertThat(delay).isEqualTo(59_998_625_000L);
        delay = rl.acquire();
        assertThat(delay).isEqualTo(59_998_125_000L);
        delay = rl.acquire();
        assertThat(delay).isEqualTo(59_997_625_000L);

    }


}

