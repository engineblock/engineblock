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

import com.codahale.metrics.Gauge;
import io.engineblock.activityapi.core.Startable;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.ActivityMetrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <H2>Synopsis</H2>
 * <p>This rate limiter uses nanoseconds as the unit of timing. This
 * works well because it is the native precision of the system timer
 * interface via {@link System#nanoTime()}. It is also low-error
 * in terms of rounding between floating point rates and nanoseconds,
 * at least in the round numbers that users tend to use. Further,
 * the current scheduling state is maintained as an atomic view of
 * accumulated nanoseconds granted to callers -- referred to here as the
 * ticks accumulator. This further simplifies the implementation by
 * allowing direct comparison of scheduled times with the current
 * state of the high-resolution system timer.
 *
 * <H2>Design Notes</H2>
 * This implementation makes certain trade-offs needed to support a combination of requirements.
 * Specifically, some small degree of inaccuracy is allowed to enable higher throughput when
 * needed. Some practical limitations affect how accurate we can be:
 *
 * <OL>
 * <LI>This is not a real-time system with guarantees on scheduling.</LI>
 * <LI>Calling overhead is significant for reading the RTC or sleeping.</LI>
 * <LI>Controlling the accuracy of a delay is not possible under any level of load.</LI>
 * <LI>It is undesirable (wasteful) to use spin loops to delay.</LI>
 * </OL>
 *
 * Together, these factors mean a compromise is inevitable. In practice it means that a very accurate
 * implementation will likely be very slow, and a very fast implementation will likely be very inaccurate.
 * This implementation tries to strike a balance, providing accuracy near the microsecond level,
 * while allowing rates in the tens of millions per second, even under heavy thread contention.
 *
 * <H2>Burst Ratio</H2>
 * <p>
 * This rate limiter provides a sliding scale between strict rate limiting and average rate limiting,
 * the difference between the two controlled by a <em>burst ratio</em> parameter. When the burst
 * ratio is 1.0, the rate limiter acts as a strict rate limiter, disallowing faster operations
 * from using time that was previously forfeited by prior slower operations. This is a "use it
 * or lose it" mode that means things like GC events can steal throughput from a running client
 * as a necessary effect of losing time in a strict timing sense.
 * </p>
 *
 * <p>
 * When the burst ratio is set to higher than 1.0, faster operations may recover lost time from
 * previously slower operations. This means that any valleys created in the actual op rate of the
 * client can be converted into plateaus of throughput above the strict rate, but only at a speed that
 * fits within (op rate * burst ratio). This allows for workloads to approximate the average
 * target rate over time, with controllable bursting rates. This ability allows for near-strict
 * behavior while allowing clients to still track truer to rate limit expectations, so long as they
 * overall workload is not saturating resources.
 * </p>
 */
public class HybridRateLimiter implements Startable, RateLimiter {

    private volatile TokenFiller filler;
    private volatile long starttime;

    // rate controls
    private RateSpec rateSpec;

    // basic state
    private ActivityDef activityDef;
    private String label;
    private State state = State.Idle;
    // metrics
    private Gauge<Long> delayGauge;
    private Gauge<Double> avgRateGauge;
    private Gauge<Double> burstRateGauge;
    private TokenPool tokens;
    // diagnostics

    // TODO Doc rate limiter scenarios, including when you want to reset the waittime, and when you don't
    private AtomicLong cumulativeWaitTimeNanos = new AtomicLong(0L);

    protected HybridRateLimiter() {
    }

    public HybridRateLimiter(ActivityDef def, String label, RateSpec rateSpec) {
        setActivityDef(def);
        setLabel(label);
        this.setRateSpec(rateSpec);
        init(activityDef);
    }

    protected void setLabel(String label) {
        this.label = label;
    }

    protected void setActivityDef(ActivityDef def) {
        this.activityDef = def;
    }

    @Override
    public long maybeWaitForOp() {
        return tokens.blockAndtake();
    }

    @Override
    public long getTotalWaitTime() {
        return this.cumulativeWaitTimeNanos.get() + getWaitTime();
    }

    @Override
    public long getWaitTime() {
        return tokens.getWaitPool();
    }

    @Override
    public RateSpec getRateSpec() {
        return this.rateSpec;
    }

    @Override
    public synchronized void setRateSpec(RateSpec updatingRateSpec) {
        if (updatingRateSpec!=null && updatingRateSpec.equals(this.rateSpec)) {
            return;
        }

        this.rateSpec = updatingRateSpec;
        this.filler = (this.filler==null) ? new TokenFiller(rateSpec):  filler.apply(rateSpec);
        this.tokens = this.filler.getTokenPool();

        switch (this.state) {
            case Started:
                sync();
            case Idle:
        }
    }

    protected void init(ActivityDef activityDef) {
        this.delayGauge = ActivityMetrics.gauge(activityDef, label + ".waittime", new RateLimiters.WaitTimeGauge(this));
        this.avgRateGauge = ActivityMetrics.gauge(activityDef, label + ".config_cyclerate", new RateLimiters.RateGauge(this));
        this.burstRateGauge = ActivityMetrics.gauge(activityDef, label + ".config_burstrate", new RateLimiters.BurstRateGauge(this));

        start();
    }

    public synchronized void start() {
        switch (state) {
            case Started:
                break;
            case Idle:
                sync();
                this.filler.start();
                state = State.Started;
                break;
        }
    }

    private synchronized void sync() {
        long nanos = getNanoClockTime();

        switch (this.state) {
            case Idle:
                this.starttime = nanos;
                this.cumulativeWaitTimeNanos.set(0L);
                break;
            case Started:
                cumulativeWaitTimeNanos.addAndGet(getWaitTime());
                break;
        }
    }

    protected long getNanoClockTime() {
        return System.nanoTime();
    }

    @Override
    public String toString() {
        return
                //"ID:"+System.identityHashCode(this)+" "+
                this.getRateSpec().toString() + "(" + this.state + ") pool:" + filler;
    }

    private enum State {
        Idle,
        Started
    }

}
