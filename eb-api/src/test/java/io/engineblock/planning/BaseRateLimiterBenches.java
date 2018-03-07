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

package io.engineblock.planning;

//import com.google.common.util.concurrent.RateLimiter;

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.DeltaHdrHistogramReservoir;
import io.engineblock.rates.RateLimiter;
import io.engineblock.rates.RateLimiters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are mostly design sanity checks to be used as needed.
 * Most of them are too expensive to run for every build.
 */
@Test(enabled=false)
public abstract class BaseRateLimiterBenches {

    protected abstract RateLimiter getRateLimiter(String paramSpec, double rate);

    // Simulated Rate=1000.0
    // Simulated Task Duration=1005000ns (+5000 extra)
    // Measured delay after 10000 tasks: 58045112
    // Measured delay per call: 5804.511
    // Extra delay overhead per call: 804.511

    // Measured delay for rate throttled limiter: 0.006153s
    @Test
    public void testCallerFastEnough() {
        long phi = 100_000_000L; // 100ms

        RateLimiter rl = RateLimiters.create(ActivityDef.parseActivityDef("alias=testing"),"1000");

        for (int i = 0; i < 2000; i++) {
            rl.acquire();
        }
        long measuredDelay = rl.getTotalSchedulingDelay();
        System.out.format("Measured delay for rate throttled limiter: %.6fs\n",((double)measuredDelay/(double)1_000_000_000));
        assertThat(measuredDelay).isLessThan(phi);
    }


    @Test
    public void testCallerTooSlowNanoLoop() {
//        DeltaHdrHistogramReservoir hist = new DeltaHdrHistogramReservoir("test", 4);

        double rate = 1000.0;
        long count = 10000;
        long addedTaskDelay = 5000;
        RateLimiter rl = getRateLimiter("alias=testing", 1000);

        int actualDelay = (int) (rl.getOpNanos() + addedTaskDelay);

        System.out.println("Simulated Rate=" + rate);
        System.out.println("Simulated Task Duration=" + actualDelay + "ns (+" + addedTaskDelay + " extra)");

        long opTicks = rl.getOpNanos();

        // Inline artificially slow task with +addedTaskDelay execution time
        for (int i = 0; i < count; i++) {
            long now = System.nanoTime();
            long then = now + actualDelay;
            long opDelay = rl.acquire();
            while (System.nanoTime() < then) {
            }
        }

        long measuredDelay = rl.getTotalSchedulingDelay();
        System.out.println("Measured delay after " + count + " tasks: " + measuredDelay);
        long expectedDelay = count * addedTaskDelay;
        double delayPerCall = (double) measuredDelay / (double) count;
        System.out.println("Measured delay per call: " + String.format("%.3f", delayPerCall));
        double extraDelayPerCall = delayPerCall - addedTaskDelay;
        System.out.println("Extra delay overhead per call: " + String.format("%.3f", extraDelayPerCall));
        assertThat(measuredDelay).isGreaterThanOrEqualTo(expectedDelay);
        long phi = 100_000_000L; // 100ms
        assertThat(measuredDelay).isLessThan(expectedDelay + phi);
    }


    /**
     * This test assumes that 100Mops/s is slow enough to make the rate
     * limiter control throttling
     */
    // stage 0: rate=1.0E7, opticks=100, delay=456093, strictness=0.0
    // stage 1: rate=1.0E7, opticks=100, delay=794113, strictness=0.0
    // stage 2: rate=1.0E7, opticks=100, delay=974498, strictness=0.0
    // stage 3: rate=1.0E7, opticks=100, delay=1008906, strictness=0.0
    // stage 4: rate=1.0E7, opticks=100, delay=526188, strictness=0.0
    // stage 5: rate=1.0E7, opticks=100, delay=1097672, strictness=0.0
    // stage 6: rate=1.0E7, opticks=100, delay=581135, strictness=0.0
    // stage 7: rate=1.0E7, opticks=100, delay=290292, strictness=0.0
    // stage 8: rate=1.0E7, opticks=100, delay=63189, strictness=0.0
    // stage 9: rate=1.0E7, opticks=100, delay=40520, strictness=0.0
    // acquires/s: 9999908.427
    // effective nanos/op: 100.000916
    @Test(enabled=false)
    public void testBlockingCostUnder() {
        double rate = 10_000_000.0d;
        int count = 100_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        RateLimiter rl = getRateLimiter("alias=testing", rate);
        long startAt = System.nanoTime();
        for (int stage = 0; stage < divisor; stage++) {
            int start = stage * stagesize;
            int end = (stage + 1) * stagesize;
            for (int i = start; i < end; i++) {
                rl.acquire();
            }
            System.out.println("stage " + stage + ": " + rl);
        }

        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
    }

    /**
     * This test assumes that 500M ops/s is too fast for the rate limiter with an empty task,
     * which will cause more short-circuit paths through acquire. The delay will increase in
     * the case that the rate doesn't keep up with the target rate.
     */
    // stage 0: rate=5.0E8, opticks=2, delay=337193834, strictness=0.0
    // stage 1: rate=5.0E8, opticks=2, delay=805649168, strictness=0.0
    // stage 2: rate=5.0E8, opticks=2, delay=1266711629, strictness=0.0
    // stage 3: rate=5.0E8, opticks=2, delay=1726300991, strictness=0.0
    // stage 4: rate=5.0E8, opticks=2, delay=2185929725, strictness=0.0
    // stage 5: rate=5.0E8, opticks=2, delay=2649233303, strictness=0.0
    // stage 6: rate=5.0E8, opticks=2, delay=3113356029, strictness=0.0
    // stage 7: rate=5.0E8, opticks=2, delay=3573784976, strictness=0.0
    // stage 8: rate=5.0E8, opticks=2, delay=4033082985, strictness=0.0
    // stage 9: rate=5.0E8, opticks=2, delay=4491281514, strictness=0.0
    // duration: 6.491
    // acquires/s: 154052155.541
    // effective nanos/op: 6.491308
    @Test
    public void testBlockingCostOver() {
        double rate = 500_000_000.0d;
        int count = 1_000_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        RateLimiter rl = getRateLimiter("alias=testing", rate);
        long startAt = System.nanoTime();
        for (int stage = 0; stage < divisor; stage++) {
            int start = stage * stagesize;
            int end = (stage + 1) * stagesize;
            for (int i = start; i < end; i++) {
                rl.acquire();
            }
            System.out.println("stage " + stage + ": " + rl);
        }

        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("duration: %.3f", duration));
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
        // Note to tester: duration of duration - (rate / count) should equal summary delay / 1E9.
    }


    // single-threaded
    // acquires/s: 203_337_793.086
    // effective nanos/op: 4.917925
    @Test
    public void testUncontendedSingleThreadedPerformance() {
        RateLimiter rl = getRateLimiter("alias=testing",1E9);
        long startAt = System.nanoTime();
        long count = 1000000000;
        for (int i = 0; i < count; i++) {
            rl.acquire();
        }
        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
    }

    /**
     * This a low-overhead test for multi-threaded access to the same rate limiter. It calculates the
     * effective concurrent rate under atomic contention.
     */
    // Running 500000000 iterations split over 200 threads at rate 500000000.000
    // limiter stats:rate=5.0E8, opticks=2, delay=60172, strictness=0.0
    // submit (200 threads)...
    // submitted (200 threads)...
    // limiter stats:rate=5.0E8, opticks=2, delay=10369703864, strictness=0.0
    // totals (seconds, cycles): (1282.506212, 500000000)
    // total thread duration: 1282.506
    // linearized acquires/s: 389861.659  (500000000 / 1282.506212)
    // linearized nanos/op: 2565.012424
    // effective concurrent acquires/s: 77972331.893
    // effective concurrent nanos/op: 12.825062
    @Test
    public void testContendedMultiThreadedPerformance() {
        int threadCount = 100;
        long iterations = 5_00_000_000L;
        double rate = 500_000_000.0d;
        int iterationsPerThread = (int) (iterations / threadCount);
        if (iterationsPerThread >= Integer.MAX_VALUE) {
            throw new RuntimeException("iterations per thread too high with (count,threads)=(" + iterations + "," + threadCount);
        }
        ExecutorService tp = Executors.newFixedThreadPool(threadCount);
        RateLimiter rl = getRateLimiter("alias=testing",rate);
        System.out.format("Running %d iterations split over %d threads at rate %.3f\n", iterations, threadCount, rate);
        BasicAcquirer[] threads = new BasicAcquirer[threadCount];
        DeltaHdrHistogramReservoir stats = new DeltaHdrHistogramReservoir("times", 5);

        // Create callables
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new BasicAcquirer(rl,iterations/threadCount);
        }

        rl.start();
        System.out.println("limiter stats:" + rl);

        System.out.format("submit (%d threads)...\n", threads.length);
        for (int i = 0; i < threadCount; i++) {
            tp.submit(threads[i]);
        }
        System.out.format("submitted (%d threads)...\n", threads.length);

        try {
            tp.shutdown();
            if (!tp.awaitTermination(1000, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to shutdown thread pool.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("limiter stats:" + rl);

        long totaltimeNs = Arrays.stream(threads)
                .map(BasicAcquirer::time)
                .mapToLong(Long::valueOf)
                .sum();
        double totalTimeSecs = (double)totaltimeNs/1_000_000_000d;
        double linearizedOpRate = (iterations / totalTimeSecs);
        double concurrentOpRate = linearizedOpRate * threadCount;

        System.out.format("totals (seconds, cycles): (%.6f, %d)\n", totalTimeSecs, iterations);
        System.out.println(String.format("total thread duration: %.3f", totalTimeSecs));
        System.out.println(String.format("linearized acquires/s: %.3f  (%d / %f)", linearizedOpRate, iterations, totalTimeSecs));
        System.out.println(String.format("linearized nanos/op: %f", (1000000000.0d / linearizedOpRate)));
        System.out.println(String.format("effective concurrent acquires/s: %.3f", concurrentOpRate));
        System.out.println(String.format("effective concurrent nanos/op: %f", (1_000_000_000D/ concurrentOpRate)));
    }

    private static class Acquirer implements Callable<AckResult>, Runnable {
        private final RateLimiter limiter;
        private final int threadIdx;
        private final DeltaHdrHistogramReservoir reservoir;
        private long iterations;

        public Acquirer(int i, RateLimiter limiter, int iterations, DeltaHdrHistogramReservoir reservoir) {
            this.threadIdx = i;
            this.limiter = limiter;
            this.iterations = iterations;
            this.reservoir = reservoir;
        }

        @Override
        public AckResult call() {
            for (int i = 0; i < iterations; i++) {
                long time = limiter.acquire();
            }
            return new AckResult(threadIdx);
        }

        @Override
        public void run() {
            for (int i = 0; i < iterations; i++) {
                limiter.acquire();
            }
        }
    }

    // 23ns per call on an i7/8(4) core system: i7-4790 CPU @ 3.60GHz
    @Test
    public void testSleepNanosRate() {
        long startAt = System.nanoTime();
        long count = 100000000;
        for (int i = 0; i < count; i++) {
            long v = System.nanoTime();
            if ((i % 10000000) == 0) {
                System.out.println("i: " + i + ", v:" + v);
            }
        }
        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("acquires/s: %.3f", (count / duration)));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
    }

    private static class BasicAcquirer implements Runnable {

        public final RateLimiter rl;
        public final long cycles;
        public long startNanos;
        public long endNanos;

        public BasicAcquirer(RateLimiter rl, long cycles) {
            this.rl = rl;
            this.cycles = cycles;
        }

        @Override
        public void run() {
            startNanos=System.nanoTime();
            for (int i = 0; i < cycles; i++) {
                rl.acquire();
            }
            endNanos=System.nanoTime();
        }

        long time() {
            return endNanos - startNanos;
        }
    }

    private static class AckResult {
        private final int threadIdx;
        public AckResult(int threadIdx) {
            this.threadIdx = threadIdx;
        }
    }

}