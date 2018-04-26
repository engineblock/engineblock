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

//import com.google.common.util.concurrent.RateLimiter;

import io.engineblock.activityapi.rates.testtypes.RateLimiterProvider;
import io.engineblock.activityapi.rates.testtypes.TestableRateLimiterProvider;
import io.engineblock.activityapi.rates.testtypes.TestableStrictRateLimiter;
import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are mostly design sanity checks to be used as needed.
 * Most of them are too expensive to run for every build.
 */
@Test
public class TestStrictRateLimiter implements RateLimiterProvider, TestableRateLimiterProvider {


    @Override
    public RateLimiter getRateLimiter(String def, String spec) {
        return new StrictRateLimiter(ActivityDef.parseActivityDef(def), "stricttesting", new RateSpec(spec));
    }

    @Override
    public TestableRateLimiter getRateLimiter(String def, String spec, AtomicLong initialClock) {
        return new TestableStrictRateLimiter(initialClock, ActivityDef.parseActivityDef(def),new RateSpec(spec));
    }

    @Test
    public void verifyShiftWithoutCarry() {
        long max = Long.MAX_VALUE>>>63;
        assertThat(max).isEqualTo(0L);
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

    @Test
    public void testRatios() {

        StrictRateLimiter crl = new StrictRateLimiter(
                ActivityDef.parseActivityDef("alias=testing"),"testing",new RateSpec(1000.0,1.0d,true));

        for (int i = 0; i < 64; i++) {
            double ratio = 1.0D/Math.pow(2.0D,i);
            int shiftby = crl.setStrictness(ratio);
            assertThat(i).isEqualTo(shiftby);
//            System.out.format("i=%2d ratio=%.023f shift=%d\n",i, ratio, shiftby);
        }
    }


    @Test(enabled=false)
    public void testStrictRateLimiterFastEnough() {
        RateLimiterPerformanceTestMethods.testCallerFastEnough(this);
    }

    @Test(enabled=false)
    public void testStrictRateLimiterCallerToSlowNanoLoop() {
        RateLimiterPerformanceTestMethods.testCallerTooSlowNanoLoop(this);
    }

    @Test(enabled=false)
    public void testStrictRateLimiterBlockingCostUnder() {
        RateLimiterPerformanceTestMethods.testBlockingCostUnder(this);
    }

    @Test(enabled=false)
    public void testStrictRateLimiterBlockingCostOver() {
        RateLimiterPerformanceTestMethods.testBlockingCostOver(this);
    }

    @Test(enabled=true)
    public void testStrictRateLimiterUncontendedSingleThreadedPerformance() {
        RateLimiterPerformanceTestMethods.testUncontendedSingleThreadedPerformance(this,1000000000L);
    }

    @Test(enabled=true)
    public void testBurstDampeningPerformanceSingleThreaded() {
        RateLimiter rl = getRateLimiter("alias=test","200000000:1.0:true");
        RateLimiterPerformanceTestMethods.testUncontendedSingleThreadedPerformance(rl,2000000000L);
        System.out.println(rl);
    }

    // effective concurrent acquires/s: 45320920.621
    // effective concurrent nanos/op: 22.064865
    // spec=opsPerSec:2.0E8, strictness:0.0, reportCoDelay:true, rateDelay=36649754214, totalDelay=36649758601
    //  (used/seen)=(187121750712125/187124435521103, clock=187158400476193, actual=187158400477845, closing=0
    //
    // effective concurrent acquires/s: 56309245.442
    // effective concurrent nanos/op: 17.759073
    // spec=opsPerSec:2.0E8, strictness:0.1, reportCoDelay:true, rateDelay=18836152238, totalDelay=28427689407
    //  (used/seen)=(187241587605852/187242943471858, clock=187260423764282, actual=187260423765227, closing=58
    //
    // effective concurrent acquires/s: 53301179.229
    // effective concurrent nanos/op: 18.761311
    // spec=opsPerSec:2.0E8, strictness:0.2, reportCoDelay:true, rateDelay=59746791, totalDelay=29837388992
    //  (used/seen)=(187336933128520/187336967077040, clock=187336992881874, actual=187336992882796, closing=293191
    //
    // effective concurrent acquires/s: 46312732.870
    // effective concurrent nanos/op: 21.592334
    // spec=opsPerSec:2.0E8, strictness:0.3, reportCoDelay:true, rateDelay=54487230, totalDelay=34826387746
    //  (used/seen)=(187414648535138/187414681736998, clock=187414703031728, actual=187414703033249, closing=12839068
    //
    // effective concurrent acquires/s: 45355049.934
    // effective concurrent nanos/op: 22.048261
    // spec=opsPerSec:2.0E8, strictness:0.4, reportCoDelay:true, rateDelay=110997446, totalDelay=35231976389
    //  (used/seen)=(187492622791225/187492703080963, clock=187492733794699, actual=187492733795595, closing=12402761
    //
    // effective concurrent acquires/s: 10266393.586
    // effective concurrent nanos/op: 97.405188
    // spec=opsPerSec:2.0E8, strictness:0.5, reportCoDelay:true, rateDelay=4101705, totalDelay=188603087771
    //  (used/seen)=(187711472273410/187711472305840, clock=187711476382246, actual=187711476383545, closing=31584376
    @Test(enabled=true)
    public void testBurstDampeningPerformanceMultiThreaded() {
        RateLimiter rl = getRateLimiter("alias=test","200000000:0.5:true");
        RateLimiterPerformanceTestMethods.testContendedMultiThreadedPerformance(rl,2000000000L, 100);
        System.out.println(rl);
    }


    @Test(enabled=true)
    public void testStrictRateLimiterContendedMultiThreadedPerformance() {
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

    @Test
    public void testHalfRatioCompensator() {
        AtomicLong clock = new AtomicLong(0L);
        TestableStrictRateLimiter rl = new TestableStrictRateLimiter(clock, ActivityDef.parseActivityDef("alias=testhalf"),new RateSpec(1000,0.5D,true));
        rl.start();
        assertThat(rl.getClock()).isEqualTo(0L);
        assertThat(rl.acquire(234)).isEqualTo(0L);
        assertThat(rl.acquire(234)).isEqualTo(0L);
        assertThat(rl.getClock()).isEqualTo(0L);

        clock.set(1_000_000_000L);
        assertThat(rl.getClock()).isEqualTo(1_000_000_000L);
        long delay = rl.acquire(1L);
//        assertThat(rl.getTicksTime()).isEqualTo(500_000_000L);

    }




}