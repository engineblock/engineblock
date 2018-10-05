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

import io.engineblock.activityapi.rates.testtypes.RateLimiterProvider;
import io.engineblock.metrics.DeltaHdrHistogramReservoir;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are mostly design sanity checks to be used as needed.
 * Most of them are too expensive to run for every build.
 * They are defined here and re-used in per-class tests in order to
 * keep testing dev flow and reporting clear and easy to follow while
 * allowing for DRY testing across rate limiter implementations.
 */
public class RateLimiterPerformanceTestMethods {

    static void testCallerFastEnough(RateLimiterProvider provider) {
        long phi = 100_000_000L; // 100ms
        int iterations=2000;

        RateLimiter rl = provider.getRateLimiter("alias=testing", "1000");

        for (int i = 0; i < iterations; i++) {
            rl.acquire();
        }
        long measuredDelay = rl.getTotalSchedulingDelay();
        System.out.format("Measured delay for rate throttled limiter: %.6fs after %d iterations\n",((double)measuredDelay/(double)1_000_000_000), iterations);
        assertThat(measuredDelay).isLessThan(phi);
    }

    static void testCallerTooSlowNanoLoop(RateLimiterProvider provider) {
        double rate = 1000.0;
        long count = 10000;
        long addedTaskDelay = 5000;
        RateLimiter rl = provider.getRateLimiter("alias=testing", "1000");
        //rl.start();

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


    static void testRateChanges(RateLimiterProvider provider, int... countsAndRates) {
        RateLimiter rl = provider.getRateLimiter("alias=testing", String.valueOf(countsAndRates[1]));

        for (int idx = 0; idx < countsAndRates.length; idx+=2) {
            int count=countsAndRates[idx];
            int rate=countsAndRates[idx+1];

            long startAt = System.nanoTime();
            rl.setRate(rate);
            for (int i = 0; i < count; i++) {
                rl.acquire();
            }
            long delay = rl.acquire();
            long endAt = System.nanoTime();
            double duration = (endAt - startAt) / 1000000000.0d;
            double acqops = (count / duration);
            System.out.println(
                    String.format(
                            "count: %9d, duration %.3f, acquires/s %.3f, nanos/op: %f, delay: %d",
                            count,duration,acqops,(1000000000.0d/acqops), delay)
            );
//            System.out.println(String.format("duration: %.3f", duration));
//            System.out.println(String.format("acquires/s: %.3f", acqops));
//            System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));

        }

    }

    /**
     * This test assumes that 100Mops/s is slow enough to make the rate
     * limiter control throttling
     */
    static void testBlockingCostUnder(RateLimiterProvider provider) {
        double rate = 10_000_000.0d;
        int count = 100_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        RateLimiter rl = provider.getRateLimiter("alias=testing", String.valueOf(rate));
        //rl.start();

        long startAt = System.nanoTime();
        long endAt = System.nanoTime();
        for (int stage = 0; stage < divisor; stage++) {
            int start = stage * stagesize;
            int end = (stage + 1) * stagesize;
            for (int i = start; i < end; i++) {
                rl.acquire();
            }
            endAt = System.nanoTime();
            System.out.println("stage " + stage + ": " + rl);
        }

 //       long endAt = System.nanoTime();
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
    static void testBlockingCostOver(RateLimiterProvider provider) {
        double rate = 500_000_000.0d;
        int count = 1_000_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        RateLimiter rl = provider.getRateLimiter("alias=testing", String.valueOf(rate));
        //rl.start;
        long startAt = System.nanoTime();
        for (int stage = 0; stage < divisor; stage++) {
            int start = stage * stagesize;
            int end = (stage + 1) * stagesize;
            System.out.println("stage " + stage + ": " + rl);
            for (int i = start; i < end; i++) {
                rl.acquire();
            }
        }

        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("duration: %.3f", duration));
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
        // Note to tester: duration of duration - (rate / count) should equal summary delay / 1E9.
    }

    static void testUncontendedSingleThreadedPerformance(RateLimiterProvider provider, long iterations) {
        RateLimiter rl = provider.getRateLimiter("alias=testing",String.valueOf(1E9));
        testUncontendedSingleThreadedPerformance(rl, iterations);
    }

    static void testUncontendedSingleThreadedPerformance(RateLimiter rl, long iterations) {
        //rl.start;
        long startAt = System.nanoTime();
//        long count = 1000000000;
        for (int i = 0; i < iterations; i++) {
            rl.acquire();
        }
        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (iterations / duration);
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
    }

    static void testContendedMultiThreadedPerformance(RateLimiterProvider provider, long iterations, int threadCount) {
        RateLimiter rl = provider.getRateLimiter("alias=testing", "500000000");
        testContendedMultiThreadedPerformance(rl, iterations, threadCount);
    }

    /**
     * This a low-overhead test for multi-threaded access to the same rate limiter. It calculates the
     * effective concurrent rate under atomic contention.
     */
    static void testContendedMultiThreadedPerformance(RateLimiter rl, long iterations, int threadCount) {
        double rate = rl.getRate();
        int iterationsPerThread = (int) (iterations / threadCount);
        if (iterationsPerThread >= Integer.MAX_VALUE) {
            throw new RuntimeException("iterations per thread too high with (count,threads)=(" + iterations + "," + threadCount);
        }
        ExecutorService tp = Executors.newFixedThreadPool(threadCount);
        System.out.format("Running %d iterations split over %d threads at rate %.3f\n", iterations, threadCount, rate);
        BasicAcquirer[] threads = new BasicAcquirer[threadCount];
        DeltaHdrHistogramReservoir stats = new DeltaHdrHistogramReservoir("times", 5);

        // Create callables
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new BasicAcquirer(rl,iterations/threadCount);
        }

        //rl.start;
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