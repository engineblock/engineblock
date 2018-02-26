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
import io.engineblock.rates.AverageRateLimiter;
import io.engineblock.rates.RateLimiter;
import io.engineblock.rates.RateLimiters;
import io.engineblock.rates.StrictRateLimiter;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are mostly design sanity checks to be used as needed.
 * Most of them are too expensive to run for every build.
 */
@Test
public class AverageRateLimiterTest {

    public static long phi = 100000000L; // 100ms

    @Test(enabled=false)
    public void testCallerFastEnough() {
        RateLimiter rl = RateLimiters.create(ActivityDef.parseActivityDef("alias=testing"),"1000");

        for (int i = 0; i < 2000; i++) {
            rl.acquire();
        }
        long measuredDelay = rl.getCumulativeSchedulingDelayNs();
        System.out.println("Measured delay: " + measuredDelay);
        assertThat(measuredDelay).isLessThan(phi);
    }

    @Test(enabled=false)
    public void testCallerTooSlowNanoLoop() {
        DeltaHdrHistogramReservoir hist = new DeltaHdrHistogramReservoir("test", 4);

        double rate = 1000.0;
        long count = 10000;
        long addedTaskDelay = 5000;
        AverageRateLimiter rl = new AverageRateLimiter(ActivityDef.parseActivityDef("alias=testing"), rate);
        int actualDelay = (int) (rl.getOpTicks() + addedTaskDelay);

        System.out.println("Simulated Rate=" + rate);
        System.out.println("Simulated Task Duration=" + actualDelay + "ns (+" + addedTaskDelay + " extra)");

        long opTicks = rl.getOpTicks();
        for (int i = 0; i < count; i++) {
            long now = System.nanoTime();
            long then = now + actualDelay;
            long opDelay = rl.acquire();
            hist.update(opDelay);
            while (System.nanoTime() < then) {
            }
        }
        System.out.println("hist:" + hist.getSnapshot().toString());
        long measuredDelay = rl.getCumulativeSchedulingDelayNs();
        System.out.println("Measured delay after " + count + " tasks: " + measuredDelay);
        long expectedDelay = count * addedTaskDelay;
        double delayPerCall = (double) measuredDelay / (double) count;
        System.out.println("Measured delay per call: " + String.format("%.3f", delayPerCall));
        double extraDelayPerCall = delayPerCall - addedTaskDelay;
        System.out.println("Extra delay overhead per call: " + String.format("%.3f", extraDelayPerCall));
        assertThat(measuredDelay).isGreaterThanOrEqualTo(expectedDelay);
        assertThat(measuredDelay).isLessThan(expectedDelay + phi);
    }

    // Synopsis: 80us (80Kns) extra logical overhead due to lack of accuracy of Lock.parkNanos(...)
    @Test(enabled=false)
    public void testCallerTooSlowParkNanos() {
        double rate = 1000.0;
        long count = 10000;
        long addedTaskDelay=500000;
        AverageRateLimiter rl = new AverageRateLimiter(ActivityDef.parseActivityDef("alias=testing"), rate);
        int actualDelay= (int) (rl.getOpTicks()+addedTaskDelay);

        System.out.println("Simulated Rate=" + rate);
        System.out.println("Simulated Task Duration=" + actualDelay +"ns (+" +addedTaskDelay + " extra)" );

        long opTicks = rl.getOpTicks();
        for (int i = 0; i < count; i++) {
            rl.acquire();
            LockSupport.parkNanos(actualDelay);
        }
        long measuredDelay = rl.getCumulativeSchedulingDelayNs();
        System.out.println("Measured delay after " + count + " tasks: " + measuredDelay);
        long expectedDelay=count*addedTaskDelay;
        double delayPerCall = (double) measuredDelay / (double) count;
        System.out.println("Measured delay per call: " + String.format("%.3f",delayPerCall));
        double extraDelayPerCall = delayPerCall - addedTaskDelay;
        System.out.println("Extra delay overhead per call: " + String.format("%.3f",extraDelayPerCall));
        assertThat(measuredDelay).isGreaterThanOrEqualTo(expectedDelay);
        assertThat(measuredDelay).isLessThan(expectedDelay+phi);
    }

    @Test(enabled=false)
    public void testBlockingCostUnder() {
        double rate = 180_000_000.0d;
        int count = 1_000_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        AverageRateLimiter rl = new AverageRateLimiter(ActivityDef.parseActivityDef("alias=testing"), rate);
        long startAt = System.nanoTime();
        for (int stage = 0; stage < divisor; stage++) {
            int start = stage * stagesize;
            int end = (stage + 1) * stagesize;
            for (int i = start; i < end; i++) {
                rl.acquire();
            }
            System.out.println("stage " + stage + ": " + rl.getSummary());
        }

        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
    }

    /**
     * This test assumes that 500M ops/s is too fast for the rate limiter with an empty task.
     */
    @Test(enabled=false)
    public void testBlockingCostOver() {
        double rate = 500_000_000.0d;
        int count = 1_000_000_000;
        int divisor = 10;
        int stagesize = count / divisor;

        AverageRateLimiter rl = new AverageRateLimiter(ActivityDef.parseActivityDef("alias=testing"), rate);
        long startAt = System.nanoTime();
        for (int stage = 0; stage < divisor; stage++) {
            int start = stage * stagesize;
            int end = (stage + 1) * stagesize;
            for (int i = start; i < end; i++) {
                rl.acquire();
            }
            System.out.println("stage " + stage + ": " + rl.getSummary());
        }

        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("duration: %.3f", duration));
        System.out.println(String.format("acquires/s: %.3f", acqops));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
        // Note to tester: duration of duration - (rate / count) should equal summary delay / 1E9.
    }


    @Test(enabled = false)
    public void testBasicRate() {
        AverageRateLimiter rl = new AverageRateLimiter(ActivityDef.parseActivityDef("alias=testing"), 1000000000.0);
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

    @Test(enabled=false)
    public void testGoogleRateLimiterRate() {
        com.google.common.util.concurrent.RateLimiter rl =
                com.google.common.util.concurrent.RateLimiter.create(1000000000);
        long startAt = System.nanoTime();
        long count = 1000000000;
        for (int i = 0; i < count; i++) {
            rl.acquire();
        }
        long endAt = System.nanoTime();
        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("acquires/s: %.3f", (count / duration)));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
        LockSupport.parkNanos(23L);
    }

    @Test(enabled=false)
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

    @Test(enabled=false)
    public void testConcurrentRateWithEmptyTask() {
        int threadCount = 100;
        long count = 500_000_000L;
        double rate = 100_000_000.0d;
        int iterationsPerThread = (int) (count / threadCount);
        if (iterationsPerThread >= Integer.MAX_VALUE) {
            throw new RuntimeException("iterations per thread too high with (count,threads)=(" + count + "," + threadCount);
        }
        ExecutorService tp = Executors.newFixedThreadPool(threadCount);
        AverageRateLimiter rl = new AverageRateLimiter(ActivityDef.parseActivityDef("alias=testing"),rate,1.0D);
        System.out.format("Running %d iterations split over %d threads at rate %.3f\n", count, threadCount, rate);
        Runnable[] threads = new Acquirer[threadCount];
        DeltaHdrHistogramReservoir stats = new DeltaHdrHistogramReservoir("times", 5);

        // Create callables
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Acquirer(i, rl, iterationsPerThread, stats);
        }
        // Submit callables
        long startAt = System.nanoTime();
        rl.start();
        System.out.println("limiter stats:" + rl);

        System.out.print("submit: ");
        for (int i = 0; i < threadCount; i++) {
            tp.submit(threads[i]);
            System.out.print("t"+i);
        }
        System.out.println();
        try {
            tp.shutdown();
            if (!tp.awaitTermination(1000, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to shutdown thread pool.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println();
        long endAt = System.nanoTime();

        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("duration: %.3f", duration));
        System.out.println(String.format("acquires/s: %.3f  (%d / %f)", (count / duration), count, duration));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
    }

    @Test(enabled=false)
    public void testConcurrentRate() {
        int threadCount = 32;
        long count = 500_000_000L;
        double rate = 100_000_000.0d;
        int iterationsPerThread = (int) (count / threadCount);
        if (iterationsPerThread >= Integer.MAX_VALUE) {
            throw new RuntimeException("iterations per thread too high with (count,threads)=(" + count + "," + threadCount);
        }
        ExecutorService tp = Executors.newFixedThreadPool(threadCount);
        AverageRateLimiter rl = new AverageRateLimiter(ActivityDef.parseActivityDef("alias=testing"),rate,1.0D);
        System.out.format("Running %d iterations split over %d threads at rate %.3f\n", count, threadCount, rate);
        Callable<AckResult>[] threads = new Acquirer[threadCount];
        ArrayList<Future<AckResult>> resultFutures = new ArrayList<>();
        DeltaHdrHistogramReservoir stats = new DeltaHdrHistogramReservoir("times", 5);

        // Create callables
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Acquirer(i, rl, iterationsPerThread, stats);
        }
        List<AckResult> resultList = new ArrayList<>();
        // Submit callables
        long startAt = System.nanoTime();
        rl.start();
        System.out.println("limiter stats:" + rl);

        System.out.print("submit: ");
        for (int i = 0; i < threadCount; i++) {
            Future<AckResult> f = tp.submit(threads[i]);
            resultFutures.add(f);
            System.out.print(i);
            System.out.flush();
        }
        System.out.println();

        System.out.print("result: ");
        for (Future<AckResult> result :resultFutures) {
            try {
                AckResult r = result.get();
                resultList.add(r);
                System.out.print(r.threadIdx);
                System.out.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println();
        long endAt = System.nanoTime();

        System.out.print("stats: ");
        for (AckResult ackResult : resultList) {
            System.out.print(ackResult.threadIdx);
        }
        System.out.println();
        System.out.println(stats.getSnapshot().toString());

        double duration = (endAt - startAt) / 1000000000.0d;
        double acqops = (count / duration);
        System.out.println(String.format("duration: %.3f", duration));
        System.out.println(String.format("acquires/s: %.3f  (%d / %f)", (count / duration), count, duration));
        System.out.println(String.format("effective nanos/op: %f", (1000000000.0d / acqops)));
    }

    private static class Acquirer implements Callable<AckResult>, Runnable {
        private final AverageRateLimiter limiter;
        private final int threadIdx;
        private final DeltaHdrHistogramReservoir reservoir;
        private long iterations;

        public Acquirer(int i, AverageRateLimiter limiter, int iterations, DeltaHdrHistogramReservoir reservoir) {
            this.threadIdx = i;
            this.limiter = limiter;
            this.iterations = iterations;
            this.reservoir = reservoir;
        }


        @Override
        public AckResult call() {
            for (int i = 0; i < iterations; i++) {
                long time = limiter.acquire();
                reservoir.update(time);
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

    private static class AckResult {
        private final int threadIdx;
        public AckResult(int threadIdx) {
            this.threadIdx = threadIdx;
        }
    }

    @Test(enabled=true)
    public void testRatios() {

        StrictRateLimiter crl = new StrictRateLimiter(ActivityDef.parseActivityDef("alias=testing"),1000L);

        for (int i = 0; i < 64; i++) {
            double ratio = 1.0D/Math.pow(2.0D,i);
            int shiftby = crl.setStrictness(ratio);
            assertThat(i).isEqualTo(shiftby);
//            System.out.format("i=%2d ratio=%.023f shift=%d\n",i, ratio, shiftby);
        }
    }

    @Test
    public void verifyShiftWithoutCarry() {
        long max = Long.MAX_VALUE>>>63;
        assertThat(max).isEqualTo(0L);
    }
}