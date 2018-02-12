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

package io.engineblock.rates;

/**
 * This RateLimiterBuilder class is meant to encapsulate the common
 * DSL for rate limiters in EngineBlock. It may return a specialized
 * rate limiter implementation based on heuristics, but this is hidden
 * from the user.
 */
public interface RateLimiterBuilder {

    /**
     * Set the rate of this rate limiter to a number of operations per second.
     * @param rate The number of operations per second
     * @return a RateLimiterBuilder.rate
     */
    WithLimit rate(double rate);

    static interface WithLimit {
        /**
         * Set the rate discipline to strict mode. Each time a time slot is scheduled,
         * the whole balance of unused time up to the last observed system time
         * will be discarded. This has the effect of requiring time slots to be
         * acquired as they become available in order to maintain the target rate,
         * disallowing any unused time to be used later. This is strict because
         * it means that any schedule delay will simply accumulate, with no
         * bursting allowed that could allow rate averaging to correct to the target
         * rate over time.
         * @return a RateLimiterBuilder.withLimit
         */
        WithStrictness withStrictLimit();

        /**
         * Set the rate limiter discipline to averaging mode. In this mode, any
         * schedule time that is unconsumed will remain available to be used by
         * operations in the future. This allows for a caller to burst arbitrarily
         * according to its capability in order to meet the rate limit target,
         * so long as it doesn't exceed the rate limit.
         * @return a RateLimiterBuilder.withLimit
         */
        WithStrictness withAverageLimit();

        /**
         * Set the rate discipline to have incremental strictness. This means that
         * each time there is extra time in the schedule after scheduling a time slot,
         * it will be partially removed. This has the effect of requiring stricter
         * timing delays from one call to the next -- sacrificing unused schedule
         * time that was previously unused.
         *
         * Internally, the amount of time to discard is calculated using register shift
         * operations. This means that all discards are 1/(2^n), based on which value
         * is closest to the fractional strictness parameter. For example,
         * strictness of 0.0 will cause no strictness, meaning that the rate limiter
         * will allow any unused time to be used later in time, thus providing average
         * rate limiting over the lifetime of the rate limiter. However, a strictness
         * parameter of 0.5 will cause half of the extra time to be discarded for each
         * call, as 0.5 yields as a shift size of 1.
         *
         * @param strictness The fractional amount of extra schedule time to remove for
         *                   each call.
         * @return A RateLimiterBuilder.strictness
         */
        WithStrictness strictness(double strictness);
    }

    static interface WithStrictness {
        /**
         * Build the rate limiter.
         * @return a {@link RateLimiter}
         */
        RateLimiter build();
    }

    static interface BuilderFacets extends RateLimiterBuilder, WithLimit, WithStrictness {}


}
