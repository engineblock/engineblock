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
import io.engineblock.activityapi.core.Startable;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.ActivityMetrics;

import java.util.concurrent.atomic.AtomicLong;

public class TokenRateLimiter implements Startable, RateLimiter {

    private AtomicLong allocatedIdealNanos = new AtomicLong(0L);
    private AtomicLong scheduledUsedNanos = new AtomicLong(0L);
    private AtomicLong availableOps = new AtomicLong(0L);
    private AtomicLong clock = new AtomicLong(0L);
    private AtomicLong cumulativeWaitTimeNanos = new AtomicLong(0L);
    private long strictNanos;
    private RateSpec rateSpec;
    private State state = State.Idle;
    private volatile long starttime;
    private Gauge<Long> delayGauge;
    private Gauge<Double> avgRateGauge;
    private Gauge<Double> burstRateGauge;
    private ActivityDef activityDef;
    private String label;
    private long maxBucketSize;

    protected TokenRateLimiter() {
    }

    public TokenRateLimiter(ActivityDef def, String label, RateSpec rateSpec) {
        setActivityDef(def);
        setLabel(label);
        this.setRateSpec(rateSpec);
        init();
    }

    protected void setLabel(String label) {
        this.label = label;
    }

    protected void setActivityDef(ActivityDef def) {
        this.activityDef = def;
    }


    @Override
    public long acquire() {
        long got = availableOps.decrementAndGet();
        if (got >= 0) {
            return clock.get() - allocatedIdealNanos.get();
        } else if (got == -1) {
            long idealNanos = allocatedIdealNanos.get();
            long neededNanos = idealNanos + strictNanos;
            long newNanos = getNanoClockTime();
            long addingNanos = newNanos - idealNanos;
            while (addingNanos < neededNanos) {
                newNanos = getNanoClockTime();
                addingNanos = newNanos - idealNanos;
            }
            clock.set(newNanos);
            addingNanos-=(addingNanos%strictNanos);
            addingNanos=Math.min(addingNanos,maxBucketSize);


            allocatedIdealNanos.addAndGet(addingNanos);
            availableOps.set((addingNanos / strictNanos)-1L); // Since this op is consuming 1, we leave it out
        } else {
            while (got <= 0) {
                got = availableOps.decrementAndGet();
                if (got <= 0) {
                    Thread.yield();
                }
            }

        }
        return clock.get() - allocatedIdealNanos.get();
    }

    @Override
    public long getTotalWaitTime() {
        return this.cumulativeWaitTimeNanos.get() + getWaitTime();
    }

    @Override
    public long getWaitTime() {
        return clock.get() - allocatedIdealNanos.get();
    }

    @Override
    public RateSpec getRateSpec() {
        return this.rateSpec;
    }

    @Override
    public void setRateSpec(RateSpec updatingRateSpec) {
        RateSpec oldRateSpec = this.rateSpec;
        this.rateSpec = updatingRateSpec;

        if (oldRateSpec != null && oldRateSpec.equals(this.rateSpec)) {
            return;
        }

        this.strictNanos = updatingRateSpec.getCalculatedNanos();
        this.maxBucketSize = ((long)updatingRateSpec.getRate()/100L)*strictNanos;
//        this.burstNanos = updatingRateSpec.getCalculatedBurstNanos();
//        this.burstWindow = (long) ((updatingRateSpec.getCalculatedNanos() / 10) * strictNanos); // 1/10 sec of ops

        switch (this.state) {
            case Started:
                sync();
            case Idle:
        }
    }

    protected void init() {
        this.delayGauge = ActivityMetrics.gauge(activityDef, label + ".waittime", new RateLimiters.WaitTimeGuage(this));
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
                state = State.Started;
                break;
        }
    }

    private synchronized void sync() {
        long nanos = getNanoClockTime();

        switch (this.state) {
            case Idle:
                this.allocatedIdealNanos.set(nanos);
                this.starttime = nanos;
                this.cumulativeWaitTimeNanos.set(0L);
//                this.scheduledNanos.set(nanos);
                break;
            case Started:
                cumulativeWaitTimeNanos.addAndGet(getWaitTime());
                break;
        }
    }

    protected long getNanoClockTime() {
        return System.nanoTime();
    }

    private enum State {
        Idle,
        Started
    }


}
