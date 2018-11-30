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

package io.engineblock.activityapi.ratelimits;

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.testutils.Perf;
import io.engineblock.testutils.Result;
import org.testng.annotations.Test;

import java.util.function.Function;

@Test(singleThreaded = true, enabled = false)
public class TestHybridRateLimiterPerf {

    private Function<RateSpec, RateLimiter> rlFunction = rs -> new HybridRateLimiter(ActivityDef.parseActivityDef("alias=tokenrl"),"hybrid", rs);
    private RateLimiterPerfTestMethods methods = new RateLimiterPerfTestMethods();

    @Test(enabled=false)
    public void testPerf1e9() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E9, 1.1),10_000_000,0.01d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e8() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E8, 1.1),50_000_000,0.005d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e7() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E7, 1.1),5_000_000,0.01d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e6() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E6, 1.1),500_000,0.005d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e5() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E5, 1.1),50_000,0.01d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e4() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E4, 1.1),5_000,0.005d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e3() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E3, 1.1),500,0.005d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e2() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E2, 1.1),50,0.005d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e1() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E1, 1.1),5,0.005d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testPerf1e0() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E0, 1.1),2,0.005d);
        System.out.println(result);
    }

    @Test(enabled=false)
    public void testePerf1eN1() {
        Result result = methods.rateLimiterSingleThreadedConvergence(rlFunction,new RateSpec(1E-1, 1.1),1,0.005d);
        System.out.println(result);

    }

    @Test(enabled=false)
    public void test100Mops_160threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 10_000_000,160);
        System.out.println(perf.getLastResult());
    }

    @Test(enabled=false)
    public void test100Mops_80threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 10_000_000,80);
        System.out.println(perf.getLastResult());
    }

    // 40 threads at 100_000_000 ops/s
    // JVM 1.8.0_152
    // 400000000_ops 29.819737_S 13413934.327_ops_s, 75_ns_op
    // 800000000_ops 60.616158_S 13197801.155_ops_s, 76_ns_op
    // JVM 11.0.1
    // 400000000_ops 33.622751_S 11896706.363_ops_s, 84_ns_op
    @Test(enabled=false)
    public void test100Mops_40threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 10_000_000,40);
        System.out.println(perf.getLastResult());
    }

    // 20 threads at 100_000_000 ops/s
    // JVM 1.8.0_152
    // 200000000_ops 14.031716_S 14253424.087_ops_s, 70_ns_op
    // 400000000_ops 35.918071_S 11136455.474_ops_s, 90_ns_op
    // 400000000_ops 30.809579_S 12982975.401_ops_s, 77_ns_op
    // 400000000_ops 36.985547_S 10815035.410_ops_s, 92_ns_op
    // 200000000_ops 16.843876_S 11873751.403_ops_s, 84_ns_op
    // 200000000_ops 17.382563_S 11505783.253_ops_s, 87_ns_op
    // JVM 11.0.1
    // 200000000_ops 12.247201_S 16330261.978_ops_s, 61_ns_op
    // 200000000_ops 15.915484_S 12566379.106_ops_s, 80_ns_op
    // 200000000_ops 17.691698_S 11304737.461_ops_s, 88_ns_op

    @Test(enabled=false)
    public void test100Mops_20threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 10_000_000,20);
        System.out.println(perf.getLastResult());
    }

    // 10 threads at 100_000_000 ops/s
    // JVM 1.8.0_152
    // 100000000_ops 5.369642_S 18623216.864_ops_s, 54_ns_op
    // 200000000_ops 16.744912_S 11943926.287_ops_s, 84_ns_op
    // 200000000_ops 17.474475_S 11445264.894_ops_s, 87_ns_op
    // 200000000_ops 14.089247_S 14195222.897_ops_s, 70_ns_op
    @Test(enabled=false)
    public void test100Mops_10threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 10_000_000,10);
        System.out.println(perf.getLastResult());
    }

    // 5 threads at 100_000_000 ops/s
    // JVM 1.8.0_152
    // 50000000_ops 2.477219_S 20183923.068_ops_s, 50_ns_op
    // 200000000_ops 10.422393_S 19189451.478_ops_s, 52_ns_op
    // 200000000_ops 10.624822_S 18823844.646_ops_s, 53_ns_op
    // JVM 11.0.1
    // 200000000_ops 11.839666_S 16892368.438_ops_s, 59_ns_op
    @Test(enabled=false)
    public void test100Mops_5threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 40_000_000,5);
        System.out.println(perf.getLastResult());
    }

}
