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
@Test
public class TestDynamicRateLimiter implements RateLimiterProvider, TestableRateLimiterProvider {

    @Override
    public RateLimiter getRateLimiter(String paramSpec, String rateSpec) {
        return new DynamicRateLimiter(ActivityDef.parseActivityDef(paramSpec),"averagetest",new RateSpec(rateSpec));
    }

    @Override
    public TestableRateLimiter getRateLimiter(String def, String spec, AtomicLong initialClock) {
        return new TestableDynamicRateLimiter(initialClock, new RateSpec(spec), ActivityDef.parseActivityDef(def));
    }

    @Test
    public void testDelayCalculations() {
        RateLimiterAccuracyTestMethods.testDelayCalculations(this);
    }

    @Test
    public void testReportedCODelayFastPath() {
        RateLimiterAccuracyTestMethods.testReportedDelayFastPath(this);
    }

    @Test
    public void testCOReportingAccuracy() {
        RateLimiterAccuracyTestMethods.testReportingAccuracy(this);
    }

    @Test
    public void testBurstCOReportingAccuracy() {
        RateLimiterAccuracyTestMethods.testBurstReportingAccuracy(this);
    }

}

