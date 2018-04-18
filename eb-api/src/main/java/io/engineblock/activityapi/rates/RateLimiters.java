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

import com.codahale.metrics.Gauge;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateLimiters {
    private final static Logger logger = LoggerFactory.getLogger(RateLimiters.class);

    public static synchronized RateLimiter createOrUpdate(ActivityDef def, String label, RateLimiter extant, RateSpec spec) {

        if (spec.strictness>0.0d) {
            if (def.getParams().getOptionalString("ratestrictness").orElse("error").equals("average")) {
                spec.strictness=0.0d;
            } else {
                throw new RuntimeException(
                        "Strict rate limiters are disabled for now. Use strictness value 0.0 instead. (example: targetrate=1K,0.0)" +
                                "See https://github.com/engineblock/engineblock/issues/148. " +
                                "Alternately, you can coerce all rate limiter strictness to 0.0 by using ratestrictness=average");
            }
        }

        if (extant==null) {
            if (spec.strictness == 0.0D) {
                logger.info("Using average rate limiter for speed: " + spec);
                return new AverageRateLimiter(def, label, spec.opsPerSec);
            } else {
                logger.info("Using strict rate limiter: " + spec);
                return new StrictRateLimiter(def, spec);
            }
        } else {
            if (extant instanceof AverageRateLimiter && spec.strictness > 0.0D) {
                AverageRateLimiter prior = (AverageRateLimiter) extant;
                logger.warn("Replacing average rate limiter with strict rate limiter:" + spec + ", beware of a performance gap." +
                        " This is due to the required logic in strict rate limiting having more overhead.");
                return new StrictRateLimiter(def, spec,prior.getTotalSchedulingDelay());
            } else {
                // Neither Strict nor Average limiting needs to be changed in an incompatible way
                extant.update(spec);
                return extant;
            }
        }
    }

    public static synchronized  RateLimiter create(ActivityDef def, String label, String specString) {
        return createOrUpdate(def, label, null,new RateSpec(specString));
    }


    public static class DelayGauge implements Gauge<Long> {

        private final RateLimiter rateLimiter;

        public DelayGauge(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }
        @Override
        public Long getValue() {
            return rateLimiter.getTotalSchedulingDelay();
        }
    }
}
