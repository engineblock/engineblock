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

import java.util.concurrent.atomic.AtomicLong;

public class BulkingRateLimiter implements RateLimiter {

    private final RateLimiter wrapped;
    private long bucketSize;
    private final static ThreadLocal<AtomicLong> TL_bucket = ThreadLocal.withInitial(() -> new AtomicLong(0L));
    private long wrappedResult;

    public BulkingRateLimiter(RateLimiter wrapped, long bucketSize) {
        this.wrapped = wrapped;
        this.bucketSize = bucketSize;
    }

    private volatile long overspeedNanos;

    @Override
    public long maybeWaitForOp() {
        AtomicLong bucket = TL_bucket.get();
        long grants = bucket.decrementAndGet();
        if (grants>=0L) {
            return wrappedResult;
        }
        wrappedResult=wrapped.maybeWaitForOps(bucketSize);
        bucket.set(bucketSize-1);
        return wrappedResult;
    }

    @Override
    public long maybeWaitForOps(long opcount) {
        return wrapped.maybeWaitForOps(opcount);
    }

    @Override
    public long getTotalWaitTime() {
        return wrapped.getTotalWaitTime();
    }

    @Override
    public long getWaitTime() {
        return wrapped.getWaitTime();
    }

    @Override
    public void applyRateSpec(RateSpec spec) {
        wrapped.applyRateSpec(spec);
    }

    @Override
    public long getStartTime() {
        return wrapped.getStartTime();
    }

    @Override
    public RateSpec getRateSpec() {
        return wrapped.getRateSpec();
    }

    @Override
    public void start() {
        wrapped.start();
    }

    public String toString() {
        return "BulkingRateLimiter(" + wrapped.toString() + ", " + bucketSize + ")";
    }
}
