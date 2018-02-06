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

import com.google.common.util.concurrent.RateLimiter;
import io.engineblock.metrics.DeltaHdrHistogramReservoir;
import org.testng.annotations.Test;

import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are mostly design sanity checks to be used as needed.
 * Most of them are too expensive to run for every build.
 */
@Test
public class CoreRateLimiterTest {

    public static long phi=100000000L; // 100ms

    @Test
    public void testCallerFastEnough() {
        CoreRateLimiter rl = new CoreRateLimiter(1000.0);
        for (int i = 0; i < 2000; i++) {
            rl.acquire();
        }
        long measuredDelay = rl.getCumulativeSchedulingDelayNs();
        System.out.println("Measured delay: " + measuredDelay);
        assertThat(measuredDelay).isLessThan(phi);
    }

    // Synopsis, about .5us (500ns) extra call overhead
    @Test
    public void testCallerTooSlowNanoLoop() {
        DeltaHdrHistogramReservoir hist = new DeltaHdrHistogramReservoir("test", 4);

        double rate = 1000.0;
        long count = 10000;
        long addedTaskDelay=5000;
        CoreRateLimiter rl = new CoreRateLimiter(rate);
        int actualDelay= (int) (rl.getOpTicks()+addedTaskDelay);

        System.out.println("Simulated Rate=" + rate);
        System.out.println("Simulated Task Duration=" + actualDelay +"ns (+" +addedTaskDelay + " extra)" );

        long opTicks = rl.getOpTicks();
        for (int i = 0; i < count; i++) {
            long now=System.nanoTime();
            long then = now+actualDelay;
            long opDelay = rl.acquire();
            hist.update(opDelay);
            while (System.nanoTime()<then) {
            }
        }
        System.out.println("hist:" + hist.getSnapshot().toString());
        long measuredDelay = rl.getCumulativeSchedulingDelayNs();
        System.out.println("Measured delay after " + count + " tasks: " + measuredDelay);
        long expectedDelay=count*addedTaskDelay;
        double delayPerCall = (double) measuredDelay / (double) count;
        System.out.println("Measured delay per call: " + String.format("%.3f",delayPerCall));
        double extraDelayPerCall = delayPerCall - addedTaskDelay;
        System.out.println("Extra delay overhead per call: " + String.format("%.3f",extraDelayPerCall));
        assertThat(measuredDelay).isGreaterThanOrEqualTo(expectedDelay);
        assertThat(measuredDelay).isLessThan(expectedDelay+phi);
    }

//    // Synopsis: 80us (80Kns) extra logical overhead due to lack of accuracy of Lock.parkNanos(...)
//    @Test
//    public void testCallerTooSlowParkNanos() {
//        double rate = 1000.0;
//        long count = 10000;
//        long addedTaskDelay=500000;
//        CoreRateLimiter rl = new CoreRateLimiter(rate);
//        int actualDelay= (int) (rl.getOpTicks()+addedTaskDelay);
//
//        System.out.println("Simulated Rate=" + rate);
//        System.out.println("Simulated Task Duration=" + actualDelay +"ns (+" +addedTaskDelay + " extra)" );
//
//        long opTicks = rl.getOpTicks();
//        for (int i = 0; i < count; i++) {
//            rl.acquire();
//            LockSupport.parkNanos(actualDelay);
//        }
//        long measuredDelay = rl.getCumulativeSchedulingDelayNs();
//        System.out.println("Measured delay after " + count + " tasks: " + measuredDelay);
//        long expectedDelay=count*addedTaskDelay;
//        double delayPerCall = (double) measuredDelay / (double) count;
//        System.out.println("Measured delay per call: " + String.format("%.3f",delayPerCall));
//        double extraDelayPerCall = delayPerCall - addedTaskDelay;
//        System.out.println("Extra delay overhead per call: " + String.format("%.3f",extraDelayPerCall));
//        assertThat(measuredDelay).isGreaterThanOrEqualTo(expectedDelay);
//        assertThat(measuredDelay).isLessThan(expectedDelay+phi);
//    }

    @Test
    public void testBlockingCostUnder() {
        double rate = 20000000.0d;
        int count=1000000000;
        int divisor=10;
        int stagesize=count/divisor;

        CoreRateLimiter rl = new CoreRateLimiter(rate);
        long startAt = System.nanoTime();
        for (int stage = 0; stage<divisor; stage++) {
            int start=stage*stagesize;
            int end=(stage+1)*stagesize;
            for (int i = start; i < end; i++) {
                rl.acquire();
            }
            System.out.println("stage " +stage +": " + rl.getSummary());
        }

        long endAt = System.nanoTime();
        double duration=(endAt-startAt)/1000000000.0d;
        double acqops = (count/duration);
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d/acqops)));
    }

    /**
     * This test assumes that 50M ops/s is too fast for the rate limiter with an empty task.
     */
    @Test
    public void testBlockingCostOver() {
        double rate = 50000000.0d;
        int count=1000000000;
        int divisor=10;
        int stagesize=count/divisor;

        CoreRateLimiter rl = new CoreRateLimiter(rate);
        long startAt = System.nanoTime();
        for (int stage = 0; stage<divisor; stage++) {
            int start=stage*stagesize;
            int end=(stage+1)*stagesize;
            for (int i = start; i < end; i++) {
                rl.acquire();
            }
            System.out.println("stage " +stage +": " + rl.getSummary());
        }

        long endAt = System.nanoTime();
        double duration=(endAt-startAt)/1000000000.0d;
        double acqops = (count/duration);
        System.out.println(String.format("duration: %.3f", duration));
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d/acqops)));
        // Note to tester: duration of duration - (rate / count) should equal summary delay / 1E9.
    }


    @Test(enabled=true)
    public void testBasicRate() {
        CoreRateLimiter rl = new CoreRateLimiter();
        long startAt = System.nanoTime();
        long count=100000000;
        for (int i = 0; i < count; i++) {
            rl.acquire();
        }
        long endAt = System.nanoTime();
        double duration=(endAt-startAt)/1000000000.0d;
        double acqops = (count/duration);
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d/acqops)));
    }

    @Test(enabled=false)
    public void testGoogleRateLimiterRate() {
        RateLimiter rl = RateLimiter.create(1000000000);
        long startAt = System.nanoTime();
        long count=1000000000;
        for (int i = 0; i < count; i++) {
            rl.acquire();
        }
        long endAt = System.nanoTime();
        double duration=(endAt-startAt)/1000000000.0d;
        double acqops = (count/duration);
        System.out.println(String.format("acquires/s: %.3f", (count/duration)));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d/acqops)));
        LockSupport.parkNanos(23L);
    }

    @Test
    public void testSleepNanosRate() {
        long startAt = System.nanoTime();
        long count=100000000;
        for (int i = 0; i < count; i++) {
            long v = System.nanoTime();
            if ((i%10000000)==0) {
                System.out.println("i: "+i+", v:"+v);
            }
        }
        long endAt=System.nanoTime();
        double duration=(endAt-startAt)/1000000000.0d;
        double acqops = (count/duration);
        System.out.println(String.format("acquires/s: %.3f", (count/duration)));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d/acqops)));
    }

}