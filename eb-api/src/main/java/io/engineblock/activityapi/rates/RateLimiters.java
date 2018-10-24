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

        if (extant == null) {
            logger.info("Using average rate limiter for speed: " + spec);
            return new DynamicRateLimiter(def, label, spec);
        } else {
            extant.setRateSpec(spec);
            return extant;
        }
    }

    public static synchronized RateLimiter create(ActivityDef def, String label, String specString) {
        return createOrUpdate(def, label, null, new RateSpec(specString));
    }

    public static class WaitTimeGuage implements Gauge<Long> {

        private final RateLimiter rateLimiter;

        public WaitTimeGuage(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        @Override
        public Long getValue() {
            return rateLimiter.getTotalWaitTime();
        }
    }

    public static class RateGauge implements Gauge<Double> {
        private final RateLimiter rateLimiter;

        public RateGauge(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        @Override
        public Double getValue() {
            return rateLimiter.getRateSpec().opsPerSec;
        }
    }

    public static class BurstRateGauge implements Gauge<Double> {
        private final RateLimiter rateLimiter;

        public BurstRateGauge(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        @Override
        public Double getValue() {
            return rateLimiter.getRateSpec().getBurstRatio() * rateLimiter.getRateSpec().getRate();
        }
    }

}
