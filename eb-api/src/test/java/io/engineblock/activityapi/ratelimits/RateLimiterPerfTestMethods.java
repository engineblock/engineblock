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

import io.engineblock.metrics.DeltaHdrHistogramReservoir;
import io.engineblock.testutils.Bounds;
import io.engineblock.testutils.Perf;
import io.engineblock.testutils.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.engineblock.util.Colors.ANSI_Blue;
import static io.engineblock.util.Colors.ANSI_Reset;

public class RateLimiterPerfTestMethods {

//    public Perf testFindOverheads(RateLimiter rl) {
//        List<Result> results = new ArrayList<>();
//        Perf perf = new Perf("perf tests for " + rl);
//        perf.add(rateLimiterSingleThreadedConvergence(rl));
////        perf.add(systemTimeOverhead(rl));
////        perf.add(conditionOverhead());
//        return perf;
//    }

    public Result systemTimeOverhead(RateLimiter rl) {
        Bounds bounds = new Bounds(1000, 2);
        Perf perf = new Perf("nanotime");

        while (!perf.isConverged(Result::getOpsPerSec, 0.01d, 3)) {
            System.out.println("testing with opcount=" + bounds.getNextValue());
            long start = System.nanoTime();
            for (long iter = 0; iter < bounds.getValue(); iter++) {
                long result = System.nanoTime();
            }
            long end = System.nanoTime();

            perf.add("nanotime/" + bounds.getValue(), start, end, bounds.getValue());
        }

        double[] deltas = perf.getDeltas(Result::getOpsPerSec);
        return perf.getLastResult();
    }

    public Result rateLimiterSingleThreadedConvergence(Function<RateSpec,RateLimiter> rlf, RateSpec rs, long startingCycles, double margin) {
        //rl.setRateSpec(rl.getRateSpec().withOpsPerSecond(1E9));
        Bounds bounds = new Bounds(startingCycles, 2);
        Perf perf = new Perf("nanotime");

        while (!perf.isConverged(Result::getOpsPerSec, margin, 3)) {
            System.out.println("testing with opcount=" + bounds.getNextValue() + " spec=" + rs);

            RateLimiter rl = rlf.apply(rs);
            long start = System.nanoTime();
            for (long iter = 0; iter < bounds.getValue(); iter++) {
                long result = rl.maybeWaitForOp();
            }
            long end = System.nanoTime();

            perf.add("rl/" + bounds.getValue(), start, end, bounds.getValue());
            System.out.println(perf.getLastResult());
        }

        return perf.getLastResult();
    }


    /**
     * This test method will call {@link  RateLimiter#maybeWaitForOp()} on a rate limiter with a sequence of different
     * getOpsPerSec specifiers. For each 4-tuple in the second varargs argument, the following fields
     * are used to control how the getOpsPerSec limiter is configured and called:
     *
     * <OL>
     * <LI>count - how many times to call maybeWaitForOp</LI>
     * <LI>getOpsPerSec - the getOpsPerSec to set the getOpsPerSec limiter to</LI>
     * <LI>divisions - the number of sub-segments to iterate and record</LI>
     * <LI>clientrate - the artificially limited client getOpsPerSec</LI>
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
            System.out.println("count=" + count + ", getOpsPerSec=" + rate + ", div=" + divisions + ", clientrate=" + clientrate);
            System.out.println("client nanos: " + clientnanos);

            long startAt = System.nanoTime();
            rl.setRateSpec(rl.getRateSpec().withOpsPerSecond(rate));
            int perDivision = count / divisions;
            long divDelay = 0L;
            for (int div = 0; div < divisions; div++) {
                long then = System.nanoTime();
                for (int i = 0; i < perDivision; i++) {
                    then += clientnanos;
                    rl.maybeWaitForOp();
                    while (System.nanoTime() < then) {
                    }
                }
                divDelay = rl.maybeWaitForOp();
                results.add(divDelay);
            }

            long endAt = System.nanoTime();
            double duration = (endAt - startAt) / 1000000000.0d;
            double acqops = (count / duration);

            System.out.println(rl.toString());

            System.out.println(ANSI_Blue +
                    String.format(
                            "spec: %s\n count: %9d, duration %.5fS, acquires/s %.3f, nanos/op: %f\n delay: %d (%.5fS)",
                            rl.getRateSpec(),
                            count, duration, acqops, (1_000_000_000.0d / acqops), divDelay, (divDelay / 1_000_000_000.0d)) +
                    ANSI_Reset);

        }

        long[] delays = results.stream().mapToLong(Long::longValue).toArray();

        String delaySummary = Arrays.stream(delays).mapToDouble(d -> (double) d / 1_000_000_000.0D).mapToObj(d -> String.format("%.3f", d))
                .collect(Collectors.joining(","));
        System.out.println("delays in seconds:\n" + delaySummary);
        System.out.println("delays in ns:\n" + Arrays.toString(delays));

        return delays;

    }

    public Result rateLimiterContendedConvergence(int threads, Function<RateSpec, RateLimiter> rlFunc, RateSpec rateSpec, int initialIterations, double margin) {
        Bounds bounds = new Bounds(initialIterations, 2);
        Perf perf = new Perf("contended with " + threads + " threads");

        while (!perf.isConverged(Result::getOpsPerSec, margin, 3)) {
            Perf delegateperf = testRateLimiterMultiThreadedContention(rlFunc, rateSpec, initialIterations, threads);
            perf.add(delegateperf.getLastResult());
        }
        return perf.getLastResult();
    }

    /**
     * This a low-overhead test for multi-threaded access to the same getOpsPerSec limiter. It calculates the
     * effective concurrent getOpsPerSec under atomic contention.
     */
    public Perf testRateLimiterMultiThreadedContention(Function<RateSpec,RateLimiter> rlFunc, RateSpec spec, long iterations, int threadCount) {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getMethodName());

        RateLimiter rl = rlFunc.apply(spec);
        double rate = spec.getRate();
        int iterationsPerThread = (int) (iterations / threadCount);
        if (iterationsPerThread >= Integer.MAX_VALUE) {
            throw new RuntimeException("iterations per thread too high with (count,threads)=(" + iterations + "," + threadCount);
        }
        RateLimiterPerfTestMethods.TestExceptionHandler errorhandler = new RateLimiterPerfTestMethods.TestExceptionHandler();
        RateLimiterPerfTestMethods.TestThreadFactory threadFactory = new RateLimiterPerfTestMethods.TestThreadFactory(errorhandler);
        ExecutorService tp = Executors.newFixedThreadPool(threadCount, threadFactory);

        System.out.format("Running %d iterations split over %d threads (%d) at getOpsPerSec %.3f\n", iterations, threadCount, (iterations / threadCount), rate);
        RateLimiterPerfTestMethods.Acquirer[] threads = new RateLimiterPerfTestMethods.Acquirer[threadCount];
        DeltaHdrHistogramReservoir stats = new DeltaHdrHistogramReservoir("times", 5);

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new RateLimiterPerfTestMethods.Acquirer(i, rl, (int) (iterations), stats, barrier);
//            threads[i] = new RateLimiterPerfTestMethods.Acquirer(i, rl, (int) (iterations / threadCount), stats, barrier);
        }

        System.out.println("limiter stats:" + rl);
        System.out.format("submitting (%d threads)...\n", threads.length);
        List<Future<Result>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(tp.submit((Callable<Result>) threads[i]));
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

        errorhandler.throwIfAny();

        System.out.println("limiter stats:" + rl);

        Perf aggregatePerf = new Perf("contended with " + threadCount + " threads for " + iterations + " iterations for " + rl.getRateSpec().toString());
        futures.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).forEachOrdered(aggregatePerf::add);

        System.out.println(aggregatePerf);

        Perf perf = aggregatePerf.reduceConcurrent();
        return perf;

//        double avgStart = results.stream().mapToLong(AckResult::getStart).average().orElseThrow(() -> new RuntimeException("No results for test"));
//        double avgEnd = results.stream().mapToLong(AckResult::getEnd).average().orElseThrow(() -> new RuntimeException("No results for test"));
//        long totalOps = iterations*threadCount;
//
//        long totaltimeNs = futures.stream().map(value -> {
//            try {
//                return value.get(60, TimeUnit.SECONDS);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }).mapToLong(RateLimiterPerfTestMethods.AckResult::time).sum();
//
////        long totaltimeNs = Arrays.stream(threads)
////                .map(BasicAcquirer::time)
////                .mapToLong(Long::valueOf)
////                .sum();
//        double totalTimeSecs = (double) totaltimeNs / 1_000_000_000d;
//        double linearizedOpRate = (iterations / totalTimeSecs);
//        double concurrentOpRate = linearizedOpRate * threadCount;
//
//        System.out.format("totals (seconds, cycles): (%.6f, %d)\n", totalTimeSecs, iterations);
//        System.out.println(String.format("total thread duration: %.3f", totalTimeSecs));
//        System.out.println(String.format("linearized acquires/s: %.3f  (%d / %f)", linearizedOpRate, iterations, totalTimeSecs));
//        System.out.println(String.format("linearized nanos/op: %f", (1000000000.0d / linearizedOpRate)));
//        System.out.println(String.format("effective concurrent acquires/s: %.3f", concurrentOpRate));
//        System.out.println(String.format("effective concurrent nanos/op: %f", (1_000_000_000D / concurrentOpRate)));
//
//        return new Perf("result for " + rl.toString()).add("result",0L,totaltimeNs,iterations);
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

    private static class Acquirer implements Callable<Result>, Runnable {
        private final RateLimiter limiter;
        private final int threadIdx;
        private final DeltaHdrHistogramReservoir reservoir;
        private final CyclicBarrier barrier;
        private long iterations;

        public Acquirer(int i, RateLimiter limiter, int iterations, DeltaHdrHistogramReservoir reservoir, CyclicBarrier barrier) {
            this.threadIdx = i;
            this.limiter = limiter;
            this.iterations = iterations;
            this.reservoir = reservoir;
            this.barrier = barrier;
        }

        @Override
        public Result call() {
//            synchronized (barrier) {
                try {
  //                  System.out.println("awaiting barrier " + this.threadIdx + " (" + barrier.getNumberWaiting() + " awaiting)");
                    barrier.await(60, TimeUnit.SECONDS);

//                    System.out.println("starting " + this.threadIdx);
                } catch (Exception be) {
                    throw new RuntimeException(be); // This should not happen unless the test is broken
                }
//            }
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                long time = limiter.maybeWaitForOp();
            }
            long endTime = System.nanoTime();
            return new Result("thread " + this.threadIdx, startTime, endTime, iterations);
        }

        @Override
        public void run() {
            for (int i = 0; i < iterations; i++) {
                limiter.maybeWaitForOp();
            }
        }
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


}
