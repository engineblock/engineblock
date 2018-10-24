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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

// TODO: rate should be volatile
// TODO: waittime should be volatile
// TODO: delay is calculated from projected expectation, not accumulated expectation

/**
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
 * This implementation makes certain trade-offs needed to support a combination of requirements.
 * Specifically, some small degree of inaccuracy is allowed to enable higher throughput when
 * needed. Some practical limitations affect how accurate we can be:
 * <OL>
 * <LI>This is not a reatime system with realtime guarantees on execution times.</LI>
 * <LI>Calling overhead is significant for reading the RTC, as well as asking a thread to delay.</LI>
 * <LI>It is undesirable (wasteful) to use spin loops to delay.</LI>
 * </OL>
 *
 * Together, these factors mean a compomise is inevitable. In practice it means that a very accurate
 * implementation will likely be very slow, and a very fast implementation will likely be very inaccurate.
 * This implementation tries to strike a balance, providing accuracy near the microsecond level,
 * while allowing rates in the tens of millions per second.
 *
 * <p>
 * This rate limiter provides a sliding scale between strict rate limiting and average rate limiting,
 * the difference between the two controlled by a <em>burst ration</em> parameter. When the burst
 * ratio is 1.0, the rate limiter acts as a strict rate limiter, disallowing faster operations
 * from using time that was previously forfeited by prior slower operations. This is a "use it
 * or lose it" mode that means things like GC events can steal throughput from a running client
 * as a necessary effect of losing time in a strict timing sense.
 *
 * When the burst ratio is set to higher than 1.0, faster operations may recover lost time from
 * previously slower operations. This means that any valleys created in the op rate of the client
 * can be converted into plateaus of throughput above the strict rate, but only at a speed that
 * fits within (op rate * burst ratio). This allows for workloads to approximate the average
 * target rate over time, with controllable bursting rates.
 */
@SuppressWarnings("ALL")
/**
 *
 * @return waittime for the op
 */

public class DynamicRateLimiter implements Startable, RateLimiter, DiagUpdateRate {
    private static final Logger logger = LoggerFactory.getLogger(DynamicRateLimiter.class);
    protected final AtomicLong allocatedIdealNanos = new AtomicLong(0L);
    // protected final AtomicLong scheduledNanos = new AtomicLong(0L);
    private final AtomicLong cumulativeWaitTimeNanos = new AtomicLong(0L);
    protected long burstWindow;
    protected volatile long clock;
    private final AtomicLong scheduledNanos = new AtomicLong(0L);
    private String label;
    private ActivityDef activityDef;
    private RateSpec rateSpec;
    private volatile long starttime;
    private long strictNanos = 0L; // Number of dynamicNanos representing one grant at target rate
    private long burstNanos = 0L; // Number of dynamicNanos representing one grant at burst rate
    private volatile long allowedGraceOps = 1L; // Number of ops to allow to proceed without sleeping to compensate for sleep overhead
    private volatile long graceOps = 0L;
    private State state = State.Idle;
    private Gauge<Long> delayGauge;
    private Gauge<Double> avgRateGauge;
    private Gauge<Double> burstRateGauge;
    private volatile long diagModulo = Long.MAX_VALUE;
//    private long calls;
//    private long sleeps;
//    private long sleeptime;
//    private long forfeits;
//    private int fastfast;
//    private int yields;
//    private int graces;

    protected DynamicRateLimiter() {
    }

    /**
     * Create a rate limiter.
     *
     * @param def      The activity definition for this rate limiter
     * @param label    The label for the rate limiting facet within the activity
     * @param rateSpec the rate limiter configuration
     */
    public DynamicRateLimiter(ActivityDef def, String label, RateSpec rateSpec) {
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

    protected void init() {
        this.delayGauge = ActivityMetrics.gauge(activityDef, label + ".waittime", new RateLimiters.WaitTimeGuage(this));
        this.avgRateGauge = ActivityMetrics.gauge(activityDef, label + ".config_cyclerate", new RateLimiters.RateGauge(this));
        this.burstRateGauge = ActivityMetrics.gauge(activityDef, label + ".config_burstrate", new RateLimiters.BurstRateGauge(this));
        start();
    }

    protected long getNanoClockTime() {
        return System.nanoTime();
    }

    // TODO: Handle scenario: If grace ops are allowed, then the behavior of toggling between ...
    // TODO ... a grouping delay and bursting should be avoided.

    // TODO: Because of desirable atomic behavior, it is difficult to make dynamic rate adjust as soon as
    // TODO: gapping is known. (There is a two-call delay). Revisit this.


    public long acquire() {

//        boolean trace = ((calls++ % diagModulo) == 0);

        long scheduledAt_IDEAL = allocatedIdealNanos.getAndAdd(strictNanos);
        long thisOpAtNanos = this.scheduledNanos.get();

        thisOpAtNanos = Math.max(thisOpAtNanos, Math.max(scheduledAt_IDEAL, clock));

        if (clock < thisOpAtNanos) {
            clock = getNanoClockTime();

            if (thisOpAtNanos < clock) {
                thisOpAtNanos = clock;
//                forfeits++;
            }
        }

        scheduledNanos.set(thisOpAtNanos + burstNanos);

        if (clock < thisOpAtNanos) { // if this op starts in the future, even with an updated clock reference
            if (graceOps++ < allowedGraceOps) {
//                graces++;
                return (thisOpAtNanos - scheduledAt_IDEAL);
            } else {
                graceOps = 0;
            }

            long sleepfor = thisOpAtNanos - clock;
            if (sleepfor > 500) { // adjusted to be closer to calling overhead of sleep
//                if (sleepfor > 1000) { // only bother with sleeping if it is in the microsecond range
//                sleeps++;
//                sleeptime += sleepfor;
//                if (trace) {
//                    System.out.println("sleeping: (thisopatnanos - clock = sleepfor)= " + thisOpAtNanos + " - " + clock + " = " + sleepfor + ": " + this.toString());
//                }
                try {
                    Thread.sleep(sleepfor / 1_000_000, (int) (sleepfor % 1_000_000));
                } catch (InterruptedException ignored) {
                } catch (IllegalArgumentException e) {

                    throw new RuntimeException("sleepfor=" + sleepfor + ": " + e.toString());
                }
            }
        }

        return (thisOpAtNanos - scheduledAt_IDEAL);
    }

    public long getOpNanos() {
        return strictNanos;
    }

    @Override
    public long getTotalWaitTime() {
        return Math.max(0, getWaitTime() + cumulativeWaitTimeNanos.get());
    }

    @Override
    public long getWaitTime() {
        return Math.max(0L, getNanoClockTime() - this.allocatedIdealNanos.get());
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
                this.scheduledNanos.set(nanos);
                break;
            case Started:
                cumulativeWaitTimeNanos.addAndGet(getWaitTime());
                break;
        }
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
        this.burstNanos = updatingRateSpec.getCalculatedBurstNanos();
        this.burstWindow = (long) ((updatingRateSpec.getCalculatedNanos() / 10) * strictNanos); // 1/10 sec of ops
        // TODO: This value has a tremendous impact on the trade-off between accuracy and performance, make it configurable and document it

        double rate = updatingRateSpec.getRate();
        long graceOpsToSet;
        if (rate <= 100000) {
            graceOpsToSet= (long) rate / 1000;
        } else if (rate <= 500000) {
            graceOpsToSet = (long) rate / 50;
        } else if (rate <= 10000000) {
            graceOpsToSet = (long) rate / 10;
        } else {
            graceOpsToSet = (long) rate / 5;
        }
        this.allowedGraceOps=Math.max(500L, graceOpsToSet);

        switch (this.state) {
            case Started:
                sync();
            case Idle:
        }
    }

    /**
     * * visible for testing
     *
     * @return allocatedOpNanos value - the long value of the shared timeslice marker
     */
    protected AtomicLong getAllocatedNanos() {
        return this.allocatedIdealNanos;
    }

    @Override
    public void setDiagModulo(long diagModulo) {
        this.diagModulo = diagModulo;
    }

    public String toString() {
        return
                String.format("spec=[%s]:%s", label, rateSpec) +
//                "\n  (∑⥁/∑(⥁⇉) (API⏲,SYS⏲)=(" +
                        String.format("\n  (⏲, ∑running)=(%d, %fS)",
                                (getNanoClockTime() - starttime),
                                (System.nanoTime() - starttime) / 1_000_000_000.0D
                        ) +
                        String.format(" (alloc sched)=(%d %d)",
                                (allocatedIdealNanos.get() - starttime),
                                (scheduledNanos.get() - starttime)
                        ) +
                        String.format(" (getwait get∑wait)=(%.3f %.3f)",
                                (getWaitTime() / 1_000_000_000.0D),
                                (this.getTotalWaitTime() / 1_000_000_000.0D)
                        )
                // String.format("\n  (calls sleeps)=(%d %d)", calls, sleeps) +
                // String.format(" (∑sleeping, %sleeping)=(%.3fS %.3fS)", ((double) sleeptime / 1_000_000_000.0d), ((double) sleeptime / 1_000_000_000.0d) * 100.0D) +
                //String.format("\n  (forfeits graces)=(%d %d %d %d %d of %d)", forfeits, graces, allowedGraceOps);
                ;
    }

    private enum State {
        Idle,
        Started
    }


}
