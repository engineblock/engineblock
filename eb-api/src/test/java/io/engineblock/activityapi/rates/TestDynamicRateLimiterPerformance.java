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
import io.engineblock.activityapi.rates.testtypes.TestableDynamicRateLimiter;
import io.engineblock.activityapi.rates.testtypes.TestableRateLimiterProvider;
import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 * These tests run all the rate limiter micro benches with average rate
 * limiting only, due to the burstRatio level being set to 0.0D.
 */
@Test(groups={"perftest"},enabled=true)
public class TestDynamicRateLimiterPerformance implements RateLimiterProvider, TestableRateLimiterProvider {

    @Override
    public RateLimiter getRateLimiter(String paramSpec, String rateSpec) {
        return new DynamicRateLimiter(ActivityDef.parseActivityDef(paramSpec),"dynamictest",new RateSpec(rateSpec));
    }

    @Override
    public TestableRateLimiter getRateLimiter(String def, String spec, AtomicLong initialClock) {
        return new TestableDynamicRateLimiter(initialClock, new RateSpec(spec), ActivityDef.parseActivityDef(def));
    }

    // Simulated Rate=1000.0
    // Simulated Task Duration=1005000ns (+5000 extra)
    // Measured delay after 10000 tasks: 58045112
    // Measured delay per call: 5804.511
    // Extra delay overhead per call: 804.511

    // Measured delay for rate throttled limiter: 0.006153s
    @Test(enabled=true)
    public void testDynamicRateLimiterFastEnough() {
        RateLimiterPerformanceTestMethods.testCallerFastEnough(this);
    }

    @Test(enabled=true)
    public void testDynamicRateLimiterCallerToSlowNanoLoop() {
        RateLimiterPerformanceTestMethods.testCallerTooSlowNanoLoop(this);
    }

    // stage 0: rate=1.0E7, opticks=100, delay=456093, burstRatio=0.0
    // stage 1: rate=1.0E7, opticks=100, delay=794113, burstRatio=0.0
    // stage 2: rate=1.0E7, opticks=100, delay=974498, burstRatio=0.0
    // stage 3: rate=1.0E7, opticks=100, delay=1008906, burstRatio=0.0
    // stage 4: rate=1.0E7, opticks=100, delay=526188, burstRatio=0.0
    // stage 5: rate=1.0E7, opticks=100, delay=1097672, burstRatio=0.0
    // stage 6: rate=1.0E7, opticks=100, delay=581135, burstRatio=0.0
    // stage 7: rate=1.0E7, opticks=100, delay=290292, burstRatio=0.0
    // stage 8: rate=1.0E7, opticks=100, delay=63189, burstRatio=0.0
    // stage 9: rate=1.0E7, opticks=100, delay=40520, burstRatio=0.0
    // acquires/s: 9999908.427
    // effective nanos/op: 100.000916
    @Test(enabled=true)
    public void testDynamicRateLimiterBlockingCostUnder() {
        RateLimiterPerformanceTestMethods.testBlockingCostUnder(this);
    }

    // stage 0: rate=5.0E8, opticks=2, delay=337193834, burstRatio=0.0
    // stage 1: rate=5.0E8, opticks=2, delay=805649168, burstRatio=0.0
    // stage 2: rate=5.0E8, opticks=2, delay=1266711629, burstRatio=0.0
    // stage 3: rate=5.0E8, opticks=2, delay=1726300991, burstRatio=0.0
    // stage 4: rate=5.0E8, opticks=2, delay=2185929725, burstRatio=0.0
    // stage 5: rate=5.0E8, opticks=2, delay=2649233303, burstRatio=0.0
    // stage 6: rate=5.0E8, opticks=2, delay=3113356029, burstRatio=0.0
    // stage 7: rate=5.0E8, opticks=2, delay=3573784976, burstRatio=0.0
    // stage 8: rate=5.0E8, opticks=2, delay=4033082985, burstRatio=0.0
    // stage 9: rate=5.0E8, opticks=2, delay=4491281514, burstRatio=0.0
    // duration: 6.491
    // acquires/s: 154052155.541
    // effective nanos/op: 6.491308

    //stage 0: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=721875667, total=721909457, (used/seen)=(17349061303153/17349116638336), (clock,actual)=(17349783217070,17349783219012)
    //stage 1: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=1391616293, total=1391619429, (used/seen)=(17349261303153/17350033505888), (clock,actual)=(17350652925866,17350652926811)
    //stage 2: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=2055345215, total=2055348149, (used/seen)=(17349461303153/17350033505888), (clock,actual)=(17351516654408,17351516655383)
    //stage 3: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=2709217193, total=2709220272, (used/seen)=(17349661303153/17350033505888), (clock,actual)=(17352370526223,17352370527189)
    //stage 4: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=3356250708, total=3356258756, (used/seen)=(17349861303153/17350033505888), (clock,actual)=(17353217564859,17353217565785)
    //stage 5: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=4015495611, total=4015498476, (used/seen)=(17350061303153/17353958650188), (clock,actual)=(17354076804430,17354076805419)
    //stage 6: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=4675343808, total=4675347399, (used/seen)=(17350261303153/17353958650188), (clock,actual)=(17354936653374,17354936654238)
    //stage 7: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=5337563749, total=5337567066, (used/seen)=(17350461303153/17353958650188), (clock,actual)=(17355798873069,17355798874281)
    //stage 8: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=6002983698, total=6002986601, (used/seen)=(17350661303153/17353958650188), (clock,actual)=(17356664300490,17356664301538)
    //stage 9: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=6663930169, total=6663933375, (used/seen)=(17350861303153/17353958650188), (clock,actual)=(17357525239369,17357525240289)
    //duration: 8.664
    //acquires/s: 115420507.225
    //effective nanos/op: 8.663972

    //stage 0: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=486127904, total=486134890, (used/seen)=(17729093742898/17729531318898), (clock,actual)=(17729579880743,17729579881655)
    //stage 1: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=943498426, total=943501262, (used/seen)=(17729293742898/17729531318898), (clock,actual)=(17730237247298,17730237248213)
    //stage 2: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=1388760200, total=1388763251, (used/seen)=(17729493742898/17729531318898), (clock,actual)=(17730882509454,17730882510472)
    //stage 3: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=1850049377, total=1850055775, (used/seen)=(17729693742898/17731003341814), (clock,actual)=(17731543803779,17731543805245)
    //stage 4: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=2305488293, total=2305491829, (used/seen)=(17729893742898/17731003341814), (clock,actual)=(17732199237436,17732199238364)
    //stage 5: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=2754297118, total=2754305027, (used/seen)=(17730093742898/17731003341814), (clock,actual)=(17732848050775,17732848051672)
    //stage 6: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=3206715308, total=3206718395, (used/seen)=(17730293742898/17731003341814), (clock,actual)=(17733500464270,17733500465143)
    //stage 7: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=3658318394, total=3658321743, (used/seen)=(17730493742898/17731003341814), (clock,actual)=(17734152067410,17734152068305)
    //stage 8: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=4103836205, total=4103839608, (used/seen)=(17730693742898/17731003341814), (clock,actual)=(17734797605587,17734797606612)
    //stage 9: spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=4549423920, total=4549452607, (used/seen)=(17730893742898/17731003341814), (clock,actual)=(17735443200336,17735443201348)
    //duration: 6.550
    //acquires/s: 152681718.615
    //effective nanos/op: 6.549573
    // after speedup with slop
    @Test(enabled=true)
    public void testDynamicRateLimiterBlockingCostOver() {
        RateLimiterPerformanceTestMethods.testBlockingCostOver(this);
    }

    // 23ns per call on an i7/8(4) core system: i7-4790 CPU @ 3.60GHz
    // 21ns per call on an i7/8(4) core system: i7-8705G CPU @ 3.10GHz
    @Test(groups="perftest",enabled=false)
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


    // single-threaded
    // acquires/s: 203_337_793.086
    // effective nanos/op: 4.917925
    @Test(groups="perftest",enabled=true)
    public void testDynamicRateLimiterUncontendedSingleThreadedPerformance() {
        RateLimiterPerformanceTestMethods.testUncontendedSingleThreadedPerformance(this,1000000000L);
    }

    @Test(groups="perftest",enabled=true)
    public void testBurstFeatureCost() {
        RateLimiter rl = new DynamicRateLimiter(ActivityDef.parseActivityDef("bursting=120"),"bursting",new RateSpec(100_000_000,1.2,true));
        RateLimiterPerformanceTestMethods.testUncontendedSingleThreadedPerformance(rl,1_000_000_000);
    }

    // Running 500000000 iterations split over 200 threads at rate 500000000.000
    // limiter stats:rate=5.0E8, opticks=2, delay=60172, burstRatio=0.0
    // submit (200 threads)...
    // submitted (200 threads)...
    // limiter stats:rate=5.0E8, opticks=2, delay=10369703864, burstRatio=0.0
    // totals (seconds, cycles): (1282.506212, 500000000)
    // total thread duration: 1282.506
    // linearized acquires/s: 389861.659  (500000000 / 1282.506212)
    // linearized nanos/op: 2565.012424
    // effective concurrent acquires/s: 77972331.893
    // effective concurrent nanos/op: 12.825062
    //
    // Running 500000000 iterations split over 100 threads at rate 500000000.000
    // limiter stats:spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=25770540, total=25781610, (used/seen)=(23839073374552/23839073374552), (clock,actual)=(23839099162050,23839099163531)
    // submit (100 threads)...
    // submitted (100 threads)...
    // limiter stats:spec=[dynamictest]:rate:5.0E8, burst:1.1, report:false, delay=6853629963, total=6853633362, (used/seen)=(23840073374552/23841239523010), (clock,actual)=(23846927011003,23846927011762)
    // totals (seconds, cycles): (520.257537, 500000000)
    // total thread duration: 520.258
    // linearized acquires/s: 961062.482  (500000000 / 520.257537)
    // linearized nanos/op: 1040.515075
    // effective concurrent acquires/s: 96106248.165
    // effective concurrent nanos/op: 10.405151

    @Test(groups="perftest",enabled=true)
    public void testDynamicRateL1imiterContendedMultiThreadedPerformance() {
        RateLimiterPerformanceTestMethods.testContendedMultiThreadedPerformance(this,500_000_000, 100);
    }

}

