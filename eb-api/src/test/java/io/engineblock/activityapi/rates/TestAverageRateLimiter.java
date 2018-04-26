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

import io.engineblock.activityapi.rates.testtypes.RateLimiterProvider;
import io.engineblock.activityapi.rates.testtypes.TestableAverageRateLimiter;
import io.engineblock.activityapi.rates.testtypes.TestableRateLimiterProvider;
import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 * These tests run all the rate limiter micro benches with average rate
 * limiting only, due to the strictness level being set to 0.0D.
 */
@Test(enabled=true)
public class TestAverageRateLimiter implements RateLimiterProvider, TestableRateLimiterProvider {

    @Override
    public RateLimiter getRateLimiter(String paramSpec, String rateSpec) {
        return new AverageRateLimiter(ActivityDef.parseActivityDef(paramSpec),"averagetest",new RateSpec(rateSpec));
    }

    @Override
    public TestableRateLimiter getRateLimiter(String def, String spec, AtomicLong initialClock) {
        return new TestableAverageRateLimiter(initialClock, new RateSpec(spec), ActivityDef.parseActivityDef(def));
    }

    // 23ns per call on an i7/8(4) core system: i7-4790 CPU @ 3.60GHz
    @Test(enabled=false)
    public void testSleepNanosRate() {
        long startAt = System.nanoTime();
        long count = 100000000;
        for (int i = 0; i < count; i++) {
            long v = System.nanoTime();
            if ((i % 10000000) == 0) {
                System.out.println("i: " + i + ", v:" + v);
            }
        }
        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("acquires/s: %.3f", (count / duration)));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
    }


    // Simulated Rate=1000.0
    // Simulated Task Duration=1005000ns (+5000 extra)
    // Measured delay after 10000 tasks: 58045112
    // Measured delay per call: 5804.511
    // Extra delay overhead per call: 804.511

    // Measured delay for rate throttled limiter: 0.006153s
    @Test()
    public void testAverageRateLimiterFastEnough() {
        RateLimiterPerformanceTestMethods.testCallerFastEnough(this);
    }

    @Test(enabled=false)
    public void testAverageRateLimiterCallerToSlowNanoLoop() {
        RateLimiterPerformanceTestMethods.testCallerTooSlowNanoLoop(this);
    }

    // stage 0: rate=1.0E7, opticks=100, delay=456093, strictness=0.0
    // stage 1: rate=1.0E7, opticks=100, delay=794113, strictness=0.0
    // stage 2: rate=1.0E7, opticks=100, delay=974498, strictness=0.0
    // stage 3: rate=1.0E7, opticks=100, delay=1008906, strictness=0.0
    // stage 4: rate=1.0E7, opticks=100, delay=526188, strictness=0.0
    // stage 5: rate=1.0E7, opticks=100, delay=1097672, strictness=0.0
    // stage 6: rate=1.0E7, opticks=100, delay=581135, strictness=0.0
    // stage 7: rate=1.0E7, opticks=100, delay=290292, strictness=0.0
    // stage 8: rate=1.0E7, opticks=100, delay=63189, strictness=0.0
    // stage 9: rate=1.0E7, opticks=100, delay=40520, strictness=0.0
    // acquires/s: 9999908.427
    // effective nanos/op: 100.000916
    @Test(enabled=false)
    public void testAverageRateLimiterBlockingCostUnder() {
        RateLimiterPerformanceTestMethods.testBlockingCostUnder(this);
    }

    // stage 0: rate=5.0E8, opticks=2, delay=337193834, strictness=0.0
    // stage 1: rate=5.0E8, opticks=2, delay=805649168, strictness=0.0
    // stage 2: rate=5.0E8, opticks=2, delay=1266711629, strictness=0.0
    // stage 3: rate=5.0E8, opticks=2, delay=1726300991, strictness=0.0
    // stage 4: rate=5.0E8, opticks=2, delay=2185929725, strictness=0.0
    // stage 5: rate=5.0E8, opticks=2, delay=2649233303, strictness=0.0
    // stage 6: rate=5.0E8, opticks=2, delay=3113356029, strictness=0.0
    // stage 7: rate=5.0E8, opticks=2, delay=3573784976, strictness=0.0
    // stage 8: rate=5.0E8, opticks=2, delay=4033082985, strictness=0.0
    // stage 9: rate=5.0E8, opticks=2, delay=4491281514, strictness=0.0
    // duration: 6.491
    // acquires/s: 154052155.541
    // effective nanos/op: 6.491308
    @Test(enabled=false)
    public void testAverageRateLimiterBlockingCostOver() {
        RateLimiterPerformanceTestMethods.testBlockingCostOver(this);
    }

    // single-threaded
    // acquires/s: 203_337_793.086
    // effective nanos/op: 4.917925
    @Test(enabled=false)
    public void testAverageRateLimiterUncontendedSingleThreadedPerformance() {
        RateLimiterPerformanceTestMethods.testUncontendedSingleThreadedPerformance(this,1000000000L);
    }

    // Running 500000000 iterations split over 200 threads at rate 500000000.000
    // limiter stats:rate=5.0E8, opticks=2, delay=60172, strictness=0.0
    // submit (200 threads)...
    // submitted (200 threads)...
    // limiter stats:rate=5.0E8, opticks=2, delay=10369703864, strictness=0.0
    // totals (seconds, cycles): (1282.506212, 500000000)
    // total thread duration: 1282.506
    // linearized acquires/s: 389861.659  (500000000 / 1282.506212)
    // linearized nanos/op: 2565.012424
    // effective concurrent acquires/s: 77972331.893
    // effective concurrent nanos/op: 12.825062
    @Test(enabled=true)
    public void testAverageRateLimiterContendedMultiThreadedPerformance() {
        RateLimiterPerformanceTestMethods.testContendedMultiThreadedPerformance(this,500_000_000, 100);
    }

    @Test
    public void testDelayCalculations() {
        RateLimiterAccuracyTestMethods.testDelayCalculations(this);
    }

    @Test
    public void testReportedCODelayFastPath() {
        RateLimiterAccuracyTestMethods.testReportedCoDelayFastPath(this);
    }

    @Test
    public void testDisabledCODelayFastPath() {
        RateLimiterAccuracyTestMethods.testDisabledCoDelayFastPath(this);
    }

    @Test
    public void testCOReportingAccuracy() {
        RateLimiterAccuracyTestMethods.testCOReportingAccuracy(this);
    }



}

