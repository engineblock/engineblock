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

@Test
public class AverageRateLimiterTest {

    /**
     * Internal delay calculations should be accurate, whether or not the caller
     * wants to know about CO-aware scheduling latency.
     */
    @Test
    public void testDelayCalculations() {
        AtomicLong clock = new AtomicLong(50_000);
        TestableAverageRateLimiter rl = new TestableAverageRateLimiter(
                clock, 1000D, 0.0, true, ActivityDef.parseActivityDef("alias=testing")
        );
        rl.start();
        assertThat(rl.getLastSeenNanoTimeline().get()).isEqualTo(50_000L);
        assertThat(rl.getTicksTimeline().get()).isEqualTo(50_000L);
        assertThat(rl.getRateSchedulingDelay()).isEqualTo(0L);
        assertThat(rl.getTotalSchedulingDelay()).isEqualTo(0L);

        clock.set(clock.get() + 50_000L);                                       // clock +50K to 100K
        assertThat(rl.getRateSchedulingDelay()).isEqualTo(50_000L);
        assertThat(rl.getTotalSchedulingDelay()).isEqualTo(50_000L);

        long l = rl.getTicksTimeline().addAndGet(25_000L);                    // +25K to 75K
        assertThat(rl.getRateSchedulingDelay()).isEqualTo(25_000L);             // 75K - 50K
        assertThat(rl.getTotalSchedulingDelay()).isEqualTo(25_000);             // 75K - 50K (+0 carryover)

        rl.acquire(20_000L);                                              // +20K to 95K
        assertThat(rl.getLastSeenNanoTimeline().get()).isEqualTo(100_000L);
        assertThat(rl.getTicksTimeline().get()).isEqualTo(95_000L);
        assertThat(rl.getRateSchedulingDelay()).isEqualTo(5000L);               // 100K-95K
        assertThat(rl.getTotalSchedulingDelay()).isEqualTo(5000L);              // 100K -95K

        clock.set(clock.get() + 100_000L);                                      // clock +100K to 200K
        rl.setRate(2000);
        assertThat(rl.getLastSeenNanoTimeline().get()).isEqualTo(200_000L);     // unchanged
        rl.acquire(1L);
        assertThat(rl.getTicksTimeline().get()).isEqualTo(200_001L);            // +1 via acquire = 200_001
        assertThat(rl.getRateSchedulingDelay()).isEqualTo(0L);                  // actually (-1) nano, but delay is 0, not negative
        assertThat(rl.getTotalSchedulingDelay()).isEqualTo(105000L);            // from before rate change, but nothing to add since
        clock.set(clock.get() + 10L);                                           // clock + 10 to 200_010
        assertThat(rl.getRateSchedulingDelay()).isEqualTo(9L);                  // was -1, but now should be +9
        assertThat(rl.getTotalSchedulingDelay()).isEqualTo(105009L);            // cumulative before rate change + current=
    }

    /**
     * When reportCoDelay is enabled, the rate limiter should always tell the
     * caller how many nanoseconds behind schedule it is according to
     * CO-aware scheduling.
     */
    @Test
    public void verifyReportedCoDelayFastPath() {
        AtomicLong clock = new AtomicLong(50_000);
        TestableAverageRateLimiter rl = new TestableAverageRateLimiter(
                clock, 1000, 0.0, true, ActivityDef.parseActivityDef("alias=testing")
        );
        rl.start();
        clock.set(clock.get() + 1000);
        long delay0 = rl.acquire(15L);
        long delay1 = rl.acquire(51L);
        long delay2 = rl.acquire(73L);
        assertThat(delay0).isEqualTo(1000L);
        assertThat(delay1).isEqualTo(985L);
        assertThat(delay2).isEqualTo(934L);

    }

    /**
     * When reportCoDelay is disabled, this rate limiter should always report back to
     * the caller that there is 0 latency added due to CO, regardless of the
     * internal measurement.
     */
    @Test
    public void verifyDisabledCoDelayFastPath() {
        AtomicLong clock = new AtomicLong(50_000);
        TestableAverageRateLimiter rl = new TestableAverageRateLimiter(
                clock, 1000, 0.0, false, ActivityDef.parseActivityDef("alias=testing")
        );
        rl.start();
        clock.set(clock.get() + 1000);
        long delay0 = rl.acquire(15L);
        long delay1 = rl.acquire(51L);
        long delay2 = rl.acquire(73L);
        assertThat(delay0).isEqualTo(0L);
        assertThat(delay1).isEqualTo(0L);
        assertThat(delay2).isEqualTo(0L);

    }

}