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

import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class TestTokenRateLimiterLogic {

    public void testLogic() {
        AtomicLong clock = new AtomicLong(0L);
        TestableTokenRateLimiter rl = new TestableTokenRateLimiter(
                clock,
                new RateSpec(1000, 1.0),
                ActivityDef.parseActivityDef("alias=testing"));

        clock.set(clock.get() + 1_000_000L);
        long waittime = rl.acquire();
        assertThat(waittime).isEqualTo(0L);

        clock.set(clock.get() + 5_000_000L);
        waittime = rl.acquire();
        assertThat(waittime).isEqualTo(0L);

        clock.set(clock.get() + 1_000_000L);
        waittime = rl.acquire();
        assertThat(waittime).isEqualTo(0L);

    }


}