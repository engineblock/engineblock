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
 *  This set is for 10M ops/s at different levels of contention.
 */
@Test(singleThreaded = true, enabled=false)
public class TestRateLimiterPerf1E7 {

    private Function<RateSpec, RateLimiter> rlFunction = rs -> new HybridRateLimiter(ActivityDef.parseActivityDef("alias=tokenrl"),"hybrid", rs.withVerb(RateSpec.Verb.configure));
    private RateLimiterPerfTestMethods methods = new RateLimiterPerfTestMethods();

    // 160 threads at 10_000_000 ops/s
    // JVM 11.0.1
    // 160000000_ops 18.122886_S 8828615.971_ops_s, 113_ns_op
    public void test10Mops_160threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E7, 1.1), 20_000_000,160);
        System.out.println(perf.getLastResult());
    }

    // 80 threads at 10_000_000 ops/s
    // JVM 11.0.1
    // 80000000_ops 8.354648_S 9575507.945_ops_s, 104_ns_op
    public void test10Mops_80threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E7, 1.1), 20_000_000,80);
        System.out.println(perf.getLastResult());
    }

    // 40 threads at 10_000_000 ops/s
    // JVM 11.0.1
    // 40000000_ops 4.001661_S 9995849.116_ops_s, 100_ns_op
    public void test10Mops_40threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E7, 1.1), 20_000_000,40);
        System.out.println(perf.getLastResult());
    }

    // 20 threads at 10_000_000 ops/s
    // JVM 11.0.1
    // 20000000_ops 1.914366_S 10447323.063_ops_s, 96_ns_op
    public void test10Mops_20threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E7, 10), 20_000_000,20);
        System.out.println(perf.getLastResult());

    }

    // 10 threads at 10_000_000 ops/s
    // JVM 11.0.1
    // 10000000_ops 0.962764_S 10386764.060_ops_s, 96_ns_op
    // 100000000_ops 9.842758_S 10159754.498_ops_s, 98_ns_op
    // 100000000_ops 10.123873_S 9877642.338_ops_s, 101_ns_op
    // 100000000_ops 10.078673_S 9921941.517_ops_s, 101_ns_op
    public void test10Mops_10threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E7, 1.1), 20_000_000,10);
        System.out.println(perf.getLastResult());
    }

    // 5 threads at 10_000_000 ops/s
    // JVM 11.0.1
    // 50000000_ops 4.804698_S 10406482.168_ops_s, 96_ns_op
    // 50000000_ops 4.923481_S 10155416.143_ops_s, 98_ns_op
    // 50000000_ops 4.924924_S 10152441.416_ops_s, 98_ns_op
    // 50000000_ops 4.924924_S 10152441.416_ops_s, 98_ns_op
    // 200000000_ops 19.761154_S 10120866.172_ops_s, 99_ns_op
    // 200000000_ops 19.928625_S 10035815.505_ops_s, 100_ns_op
    public void test10Mops_5threads() {
        Perf perf = methods.testRateLimiterMultiThreadedContention(rlFunction, new RateSpec(1E7, 1.1), 20_000_000,5);
        System.out.println(perf.getLastResult());
    }

}
