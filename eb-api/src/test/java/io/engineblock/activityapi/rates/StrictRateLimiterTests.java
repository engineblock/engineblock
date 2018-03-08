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

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityapi.rates.StrictRateLimiter;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are mostly design sanity checks to be used as needed.
 * Most of them are too expensive to run for every build.
 */
@Test
public class StrictRateLimiterTests {


    @Test
    public void verifyShiftWithoutCarry() {
        long max = Long.MAX_VALUE>>>63;
        assertThat(max).isEqualTo(0L);
    }

    @Test
    public void testRatios() {

        StrictRateLimiter crl = new StrictRateLimiter(ActivityDef.parseActivityDef("alias=testing"),1000L);

        for (int i = 0; i < 64; i++) {
            double ratio = 1.0D/Math.pow(2.0D,i);
            int shiftby = crl.setStrictness(ratio);
            assertThat(i).isEqualTo(shiftby);
//            System.out.format("i=%2d ratio=%.023f shift=%d\n",i, ratio, shiftby);
        }
    }




}