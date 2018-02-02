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

package io.engineblock.planning;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimiterCOAwareTest {

    public static long phi=100000000L; // 100ms

    @Test
    public void testCallerFastEnough() {
        RateLimiterCOAware rl = new RateLimiterCOAware(1000.0);
        for (int i = 0; i < 2000; i++) {
            rl.acquire();
        }
        long measuredDelay = rl.getDelayNanos();
        System.out.println("Measured delay: " + measuredDelay);
        assertThat(measuredDelay).isLessThan(phi);
    }

    @Test
    public void testCallerTooSlow() {
        double rate = 1000.0;
        long count = 1000;
        long addlDelayNs=5000;
        long expectedDelay=count*addlDelayNs;
        RateLimiterCOAware rl = new RateLimiterCOAware(rate);
        int actualDelay= (int) (rl.getOpTicks()+addlDelayNs);

        System.out.println("actualDelay=" + actualDelay +"ns");

        long opTicks = rl.getOpTicks();
        for (int i = 0; i < count; i++) {
            rl.acquire();
            try {
                Thread.sleep(actualDelay/1000000,actualDelay%1000000);
            } catch (InterruptedException ignored) { }
        }
        long measuredDelay = rl.getDelayNanos();
        System.out.println("Measured delay: " + measuredDelay);
        assertThat(measuredDelay).isGreaterThanOrEqualTo(expectedDelay);
        assertThat(measuredDelay).isLessThan(expectedDelay+phi);
    }

}