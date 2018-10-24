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
import io.engineblock.metrics.DeltaHdrHistogramReservoir;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static io.engineblock.util.Colors.Blue;
import static io.engineblock.util.Colors.Reset;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are disabled by default because they are not really unit tests. They
 * are used to verify expected behavior of the rate limiter implementation under
 * artificial load, and can be uncommented as needed during rate limiter development.
 */
@Test(groups = {"perftest"}, enabled = true)
public class TestRateLimiterPerformance {

    @Test(groups = {"perftest"}, enabled = false)
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


    @Test(groups = {"perftest"}, enabled = false)
    void testCallerFastEnough() {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());
        long phi = 100_000_000L; // 100ms
        int iterations = 2000;

        DynamicRateLimiter rl = new DynamicRateLimiter(
                ActivityDef.parseActivityDef("alias=testing"), "dynamictest", new RateSpec("1000"));

        for (int i = 0; i < iterations; i++) {
            rl.acquire();
        }
        long measuredDelay = rl.getTotalWaitTime();
        System.out.format("Measured delay for rate throttled limiter: %.6fs after %d iterations\n", ((double) measuredDelay / (double) 1_000_000_000), iterations);
        assertThat(measuredDelay).isLessThan(phi);
    }


    @Test(groups = {"perftest"}, enabled = false)
    void testCallerTooSlowNanoLoop() {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());
        double rate = 1000.0;
        long count = 10000;
        long addedTaskDelay = 5000;
        RateLimiter rl = new DynamicRateLimiter(
                ActivityDef.parseActivityDef("alias=testing"),
                "testing",
                new RateSpec("1000")
        );
        //rl.start();

        int actualDelay = (int) (rl.getRateSpec().getCalculatedNanos() + addedTaskDelay);

        System.out.println("Simulated Rate=" + rate);
        System.out.println("Simulated Task Duration=" + actualDelay + "ns (+" + addedTaskDelay + " extra)");

        long opTicks = rl.getRateSpec().getCalculatedNanos();

        // Inline artificially slow task with +addedTaskDelay execution time
        for (int i = 0; i < count; i++) {
            long now = System.nanoTime();
            long then = now + actualDelay;
            long opDelay = rl.acquire();
            while (System.nanoTime() < then) {
            }
        }

        long measuredDelay = rl.getTotalWaitTime();
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
     * This test calls a rate limiter at 0.5 the target rate, and then at 1.5 the target rate,
     * taking samples of the waittime at intervals, and checking that they are accurate within
     * a reasonable margin.
     *
     * Because this uses a single-threaded test client which has an inefficient sleep loop,
     * among other reasons, this test is not indicative of actual achievable performance.
     * It is merely used to validate that the waittime calculations and bursting ratios
     * are effective when running in a reproducible scenario at some speed.
     */
    @Test(groups = {"perftest"}, enabled = false)
    public void testRateChangesSmall() {
        DynamicRateLimiter rl = new DynamicRateLimiter(ActivityDef.parseActivityDef("alias=testing"), "testing", new RateSpec("1000,2.01"));
        long[] delays = testRateChanges(
                rl,
                1_000, 500, 4, 250,
                2_000, 500, 4, 3_000
        );

        assertThat(delays[3]).isBetween(1_900_000_000L, 2_100_000_000L); // 2 seconds +- 0.1 seconds
        assertThat(delays[7]).isBetween(-100_000_000L, 100_000_000L); // 0 seconds +- 0.1 seconds

    }

    /**
     * See {@link #testRateChangesSmall()}
     */
    @Test(groups = {"perftest"}, enabled = false)
    public void testRateChangesMedium() {
        DynamicRateLimiter rl = new DynamicRateLimiter(ActivityDef.parseActivityDef("alias=testing"), "testing", new RateSpec("1000,2.01"));
        long[] delays = testRateChanges(
                rl,
                100_000, 50_000, 4, 25_000,
                200_000, 50_000, 4, 300_000
        );

        assertThat(delays[3]).isBetween(1_900_000_000L, 2_100_000_000L); // 2 seconds +- 0.1 seconds
        assertThat(delays[7]).isBetween(-100_000_000L, 100_000_000L); // 0 seconds +- 0.1 seconds
    }


    /**
     * See {@link #testRateChangesSmall()}
     */
    @Test(groups = {"perftest"}, enabled = false)
    public void testRateChangesLarge() {
        DynamicRateLimiter rl = new DynamicRateLimiter(ActivityDef.parseActivityDef("alias=testing"), "testing", new RateSpec("500000,2.1"));
        long[] delays = testRateChanges(
                rl,
                1_000_000, 500_000, 4, 250_000,
                2_000_000, 500_000, 4, 2_000_000
        );
        assertThat(delays[3]).isBetween(1_900_000_000L, 2_100_000_000L);
        assertThat(delays[7]).isBetween(-1_000_000L, 1_000_000L);
    }

    /**
     * See {@link #testRateChangesSmall()}
     */
    @Test(groups = {"perftest"}, enabled = false)
    public void testRateChangesExtraLarge() {
        DynamicRateLimiter rl = new DynamicRateLimiter(ActivityDef.parseActivityDef("alias=testing"), "testing", new RateSpec("500000,2.1"));
        long[] delays = testRateChanges(
                rl,
                10_000_000, 5_000_000, 4, 2_500_000,
                20_000_000, 5_000_000, 4, 20_000_000
        );
        assertThat(delays[3]).isBetween(1_900_000_000L, 2_100_000_000L);
        assertThat(delays[7]).isBetween(-1_000_000L, 1_000_000L);
    }

    /**
     * This test method will call acquire on a rate limiter with a sequence of different
     * rate specifiers. For each 4-tuple in the second varargs argument, the following fields
     * are used to control how the rate limiter is configured and called:
     *
     * <OL>
     * <LI>count - how many times to call acquire</LI>
     * <LI>rate - the rate to set the rate limiter to</LI>
     * <LI>divisions - the number of sub-segments to iterate and record</LI>
     * <LI>clientrate - the artificially limited client rate</LI>
     * </OL>
     *
     * @param count_rate_division_clientrate
     * @return
     */
    long[] testRateChanges(RateLimiter rl, int... count_rate_division_clientrate) {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());

        List<Long> results = new ArrayList<>();

        for (int idx = 0; idx < count_rate_division_clientrate.length; idx += 4) {
            int count = count_rate_division_clientrate[idx];
            int rate = count_rate_division_clientrate[idx + 1];
            int divisions = count_rate_division_clientrate[idx + 2];
            int clientrate = count_rate_division_clientrate[idx + 3];
            long clientnanos = (long) (1_000_000_000.0D / clientrate);

            if (rl instanceof DiagUpdateRate) {
                ((DiagUpdateRate) rl).setDiagModulo(count / divisions);
                System.out.println("updating every " + (count / divisions) + " calls (" + count + "/" + divisions + ")");
            }
            System.out.println("count=" + count + ", rate=" + rate + ", div=" + divisions + ", clientrate=" + clientrate);
            System.out.println("client nanos: " + clientnanos);

            long startAt = System.nanoTime();
            rl.setRateSpec(rl.getRateSpec().withOpsPerSecond(rate));
            int perDivision = count / divisions;
            long divDelay = 0L;
            for (int div = 0; div < divisions; div++) {
                long then = System.nanoTime();
                for (int i = 0; i < perDivision; i++) {
                    then += clientnanos;
                    rl.acquire();
                    while (System.nanoTime() < then) {
                    }
                }
                divDelay = rl.acquire();
                results.add(divDelay);
            }

            long endAt = System.nanoTime();
            double duration = (endAt - startAt) / 1000000000.0d;
            double acqops = (count / duration);

            System.out.println(rl.toString());

            System.out.println(Blue +
                    String.format(
                            "spec: %s\n count: %9d, duration %.5fS, acquires/s %.3f, nanos/op: %f\n delay: %d (%.5fS)",
                            rl.getRateSpec(),
                            count, duration, acqops, (1_000_000_000.0d / acqops), divDelay, (divDelay / 1_000_000_000.0d)) +
                    Reset);

        }

        long[] delays = results.stream().mapToLong(Long::longValue).toArray();

        String delaySummary = Arrays.stream(delays).mapToDouble(d -> (double) d / 1_000_000_000.0D).mapToObj(d -> String.format("%.3f", d))
                .collect(Collectors.joining(","));
        System.out.println("delays in seconds:\n" + delaySummary);
        System.out.println("delays in ns:\n" + Arrays.toString(delays));

        return delays;

    }

    @Test(groups = {"perftest"}, enabled = false)
    void testNoSleeping() {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());

        double rate = 100_000_000.0d;
        int count = 100_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        RateLimiter rl = new DynamicRateLimiter(
                ActivityDef.parseActivityDef("alias=testing"),
                "testing",
                new RateSpec(String.valueOf(rate))
        );

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

    @Test(groups = {"perftest"}, enabled = false)
    void testSleepingAccuracy() {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());

        double rate = 10_000_000.0d;
        int count = 100_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        RateLimiter rl = new DynamicRateLimiter(
                ActivityDef.parseActivityDef("alias=testing"),
                "testing",
                new RateSpec(String.valueOf(rate))
        );

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
    @Test(groups = {"perftest"}, enabled = false)
    void testBlockingCostOver() {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());

        double rate = 500_000_000.0d;
        int count = 1_000_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        RateLimiter rl = new DynamicRateLimiter(
                ActivityDef.parseActivityDef("alias=testing"),
                "testing",
                new RateSpec(String.valueOf(rate))
        );

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

    @Test(groups = {"perftest"}, enabled = false)
    public void testDynamicRateLimiterUncontendedSingleThreadedPerformance() {
        DynamicRateLimiter rl = new DynamicRateLimiter(
                ActivityDef.parseActivityDef("alias=testing"),
                "testing", new RateSpec("500000000")
        );
        testUncontendedSingleThreadedPerformance(rl, 500_000_000);
    }


    private void testUncontendedSingleThreadedPerformance(RateLimiter rl, long iterations) {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    @Test(groups = {"perftest"}, enabled = false)
    public void testDynamicRateLimiterContendedMultiThreadedPerformance() {
        DynamicRateLimiter rl = new DynamicRateLimiter(
                ActivityDef.parseActivityDef("alias=testing"),
                "testing", new RateSpec("500000000")
        );
        testContendedMultiThreadedPerformance(rl, 500_000_000, 200);
    }

    /**
     * This a low-overhead test for multi-threaded access to the same rate limiter. It calculates the
     * effective concurrent rate under atomic contention.
     */
    private void testContendedMultiThreadedPerformance(RateLimiter rl, long iterations, int threadCount) {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());

        double rate = rl.getRateSpec().getRate();
        int iterationsPerThread = (int) (iterations / threadCount);
        if (iterationsPerThread >= Integer.MAX_VALUE) {
            throw new RuntimeException("iterations per thread too high with (count,threads)=(" + iterations + "," + threadCount);
        }
        TestExceptionHandler errorhandler = new TestExceptionHandler();
        TestThreadFactory threadFactory = new TestThreadFactory(errorhandler);
        ExecutorService tp = Executors.newFixedThreadPool(threadCount, threadFactory);

        System.out.format("Running %d iterations split over %d threads (%d) at rate %.3f\n", iterations, threadCount, (iterations / threadCount), rate);
        Acquirer[] threads = new Acquirer[threadCount];
        DeltaHdrHistogramReservoir stats = new DeltaHdrHistogramReservoir("times", 5);

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Acquirer(i, rl, (int) (iterations / threadCount), stats, stats);
        }
        System.out.println("limiter stats:" + rl);

        System.out.format("submit (%d threads)...\n", threads.length);
        List<Future<AckResult>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(tp.submit((Callable<AckResult>) threads[i]));
        }
        System.out.format("submitted (%d threads)...\n", threads.length);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        } // if this takes longer than a second something is very wrong
        synchronized (stats) {
            stats.notifyAll();
        }

        try {
            tp.shutdown();
            if (!tp.awaitTermination(1000, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to shutdown thread pool.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        errorhandler.throwIfAny();

        System.out.println("limiter stats:" + rl);
        long totaltimeNs = futures.stream().map(value -> {
            try {
                return value.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).mapToLong(AckResult::time).sum();

//        long totaltimeNs = Arrays.stream(threads)
//                .map(BasicAcquirer::time)
//                .mapToLong(Long::valueOf)
//                .sum();
        double totalTimeSecs = (double) totaltimeNs / 1_000_000_000d;
        double linearizedOpRate = (iterations / totalTimeSecs);
        double concurrentOpRate = linearizedOpRate * threadCount;

        System.out.format("totals (seconds, cycles): (%.6f, %d)\n", totalTimeSecs, iterations);
        System.out.println(String.format("total thread duration: %.3f", totalTimeSecs));
        System.out.println(String.format("linearized acquires/s: %.3f  (%d / %f)", linearizedOpRate, iterations, totalTimeSecs));
        System.out.println(String.format("linearized nanos/op: %f", (1000000000.0d / linearizedOpRate)));
        System.out.println(String.format("effective concurrent acquires/s: %.3f", concurrentOpRate));
        System.out.println(String.format("effective concurrent nanos/op: %f", (1_000_000_000D / concurrentOpRate)));
    }


    private static class TestThreadFactory implements ThreadFactory {

        private final Thread.UncaughtExceptionHandler handler;

        public TestThreadFactory(Thread.UncaughtExceptionHandler uceh) {
            this.handler = uceh;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler(handler);
            return t;
        }
    }

    private static class TestExceptionHandler implements Thread.UncaughtExceptionHandler {
        public List<Throwable> throwables = new ArrayList<>();
        public List<Thread> threads = new ArrayList<>();

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            threads.add(t);
            throwables.add(e);
            System.out.println("uncaught exception on thread " + t.getName() + ": " + e.toString());
        }

        public void throwIfAny() {
            if (throwables.size() > 0) {
                throw new RuntimeException(throwables.get(0));
            }
        }
    }

    private static class Acquirer implements Callable<AckResult>, Runnable {
        private final RateLimiter limiter;
        private final int threadIdx;
        private final DeltaHdrHistogramReservoir reservoir;
        private final Object monitor;
        private long iterations;

        public Acquirer(int i, RateLimiter limiter, int iterations, DeltaHdrHistogramReservoir reservoir, Object monitor) {
            this.threadIdx = i;
            this.limiter = limiter;
            this.iterations = iterations;
            this.reservoir = reservoir;
            this.monitor = monitor;
        }

        @Override
        public AckResult call() {
            synchronized (monitor) {
                try {
                    monitor.wait(5000);
                } catch (InterruptedException ignored) {
                }
            }
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                long time = limiter.acquire();
            }
            long endTime = System.nanoTime();
            return new AckResult(threadIdx, endTime - startTime);
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
        private final Object monitor;
        public long startNanos;
        public long endNanos;

        public BasicAcquirer(RateLimiter rl, long cycles, Object monitor) {
            this.rl = rl;
            this.cycles = cycles;
            this.monitor = monitor;
        }

        @Override
        public void run() {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException ignored) {
                }
            }
            startNanos = System.nanoTime();
            System.out.println("starting: " + Thread.currentThread().getName());
            for (int i = 0; i < cycles; i++) {
                rl.acquire();
            }
            System.out.println("stopping: " + Thread.currentThread().getName());
            endNanos = System.nanoTime();
        }

        long time() {
            return endNanos - startNanos;
        }
    }

    private static class AckResult {
        private final int threadIdx;
        private long time;

        public AckResult(int threadIdx, long time) {
            this.threadIdx = threadIdx;
            this.time = time;
        }

        public long time() {
            return time;
        }
    }

}

