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
import org.testng.annotations.Test;

import java.util.function.Function;

/**
 * These tests are for sanity checking rate limiter implementations. They are not enabled by default,
 * since they are very CPU an time intensive. If you are developing rate limiters, use these to understand
 * throughput variations at different speeds and different levels of contention.
 *
 * This set is for 100M ops/s at different levels of contention.
 */
@Test(singleThreaded = true, enabled=false)
public class TestRateLimiterPerf1E8 {

    private Function<RateSpec, RateLimiter> rlFunction = rs -> new HybridRateLimiter(ActivityDef.parseActivityDef("alias=tokenrl"),"hybrid", rs);
    private RateLimiterPerfTestMethods methods = new RateLimiterPerfTestMethods();

    // 160 threads at 100_000_000 ops/s
    // 1600000000_ops 149.351811_S 10712960.186_ops_s, 93_ns_op
    // JVM 11.0.1, Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz
    // 1600000000_ops 160.319831_S 9_980_050.444_ops_s, 100_ns_op
    // 1600000000_ops 159.234501_S 10_048_073.673_ops_s, 100_ns_op
    // 1600000000_ops 158.224286_S 10_112_227.620_ops_s, 99_ns_op
    //
    @Test(enabled=false)
    public void test100Mops_160threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 10_000_000,160);
        System.out.println(perf.getLastResult());
    }

    // 80 threads at 100_000_000 ops/s
    // JVM 11.0.1, Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz
    // 800000000_ops 74.104295_S 10795595.534_ops_s, 93_ns_op
    // 800000000_ops 74.155495_S 10788141.933_ops_s, 93_ns_op
    @Test(enabled=false)
    public void test100Mops_80threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 10_000_000,80);
        System.out.println(perf.getLastResult());
    }

    // 40 threads at 100_000_000 ops/s
    // JVM 1.8.0_152
    // 400000000_ops 29.819737_S 13413934.327_ops_s, 75_ns_op
    // 800000000_ops 60.616158_S 13197801.155_ops_s, 76_ns_op
    // JVM 11.0.1, Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz
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
    // JVM 11.0.1, Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz
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
    // JVM 11.0.1, Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz
    // 100000000_ops 7.751758_S 12900299.587_ops_s, 78_ns_op
    // 100000000_ops 7.864851_S 12714799.657_ops_s, 79_ns_op
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
    // JVM 11.0.1, Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz
    // 200000000_ops 11.839666_S 16892368.438_ops_s, 59_ns_op
    // 50000000_ops 2.390485_S 20916254.150_ops_s, 48_ns_op
    // 100000000_ops 6.317008_S 15830279.182_ops_s, 63_ns_op
    // 200000000_ops 13.551712_S 14758282.931_ops_s, 68_ns_op
    @Test(enabled=false)
    public void test100Mops_5threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E8, 1.1), 40_000_000,5);
        System.out.println(perf.getLastResult());
    }

}
