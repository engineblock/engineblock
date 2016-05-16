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

package io.engineblock.script;

import io.engineblock.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScenariosExecutor {

    private final static Logger logger = LoggerFactory.getLogger(ScenariosExecutor.class);

    private LinkedHashMap<Scenario, Future<Result>> submittedScenarios = new LinkedHashMap<>();
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    public ScenariosExecutor() {
    }

    public ScenariosExecutor(int threads) {
        executor = Executors.newFixedThreadPool(threads);
    }

    public void execute(Scenario scenario) {
        Future<Result> submitted = executor.submit(scenario);
        submittedScenarios.put(scenario, submitted);
    }

    /**
     * Shuts down all running scenarios and awaits all results.
     * @return the final scenario-result map.
     */
    public Map<Scenario, Result> awaitAllResults() {
        return awaitAllResults(Long.MAX_VALUE/2, 60000); // half max value, to avoid overflow
    }

    /**
     * Shuts down all running scenarios and awaits all results.
     * @param timeout how long to wait for the results to complete
     * @param updateInterval how frequently to log status while waiting
     * @return the final scenario-result map
     */
    public Map<Scenario, Result> awaitAllResults(long timeout, long updateInterval) {
        if (updateInterval > timeout) {
            throw new InvalidParameterException("timeout must be equal to or greater than updateInterval");
        }
        long timeoutAt = System.currentTimeMillis() + timeout;

        executor.shutdown();
        boolean isShutdown = false;

        while (!isShutdown && System.currentTimeMillis() < timeoutAt) {
            long updateAt = Math.min(timeoutAt, System.currentTimeMillis() + updateInterval);
            long waitedAt = System.currentTimeMillis();
            while (!isShutdown && System.currentTimeMillis() < timeoutAt) {

                while (!isShutdown && System.currentTimeMillis() < updateAt) {
                    try {
                        long timeRemaining = timeoutAt - System.currentTimeMillis();
                        logger.debug("Waiting for timeRemaining:" + timeRemaining + "ms for scenarios executor to shutdown.");
                        isShutdown = executor.awaitTermination(timeRemaining, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                    }
                }
                updateAt = Math.min(timeoutAt, System.currentTimeMillis() + updateInterval);
            }

            logger.info("scenarios executor shutdown after " + (System.currentTimeMillis()-waitedAt) + "ms.");
        }

        if (!isShutdown) {
            throw new RuntimeException("executor still runningScenarios after awaiting all results for " + timeout
                    + "ms.  isTerminated:" + executor.isTerminated() + " isShutdown:" + executor.isShutdown());
        }
        Map<Scenario, Result> results = new LinkedHashMap<Scenario,Result>();
        getAsyncResultStatus()
                .entrySet().stream()
                .forEach(es -> results.put(es.getKey(), es.getValue().orElseGet(null)));
        return results;
    }

    /**
     * @return list of scenarios which have been submitted, in order
     */
    public List<String> getPendingScenarios() {
        return new ArrayList<String>(
                submittedScenarios.keySet().stream().map(Scenario::getName).collect(Collectors.toCollection(ArrayList::new))
        );
    }

    /**
     * <p>Returns a map of all pending scenario names and optional results.
     * All submitted scenarios are included. Those which are still pending
     * are returned with an empty option.</p>
     *
     * <p>Results may be exceptional. If {@link Result#getException()} is present,
     * then the result did not complete normally.</p>
     *
     * @return map of async results, with incomplete results as Optional.empty()
     */
    public Map<Scenario, Optional<Result>> getAsyncResultStatus() {
        Map<Scenario, Optional<Result>> optResults = new LinkedHashMap<>();
        for (Scenario pendingScenario : submittedScenarios.keySet()) {
            Future<Result> resultFuture = submittedScenarios.get(pendingScenario);

            Optional<Result> oResult = Optional.empty();
            if (resultFuture.isDone()) {
                try {
                    oResult = Optional.of(resultFuture.get());
                } catch (Exception e) {
                    oResult = Optional.of(new Result(e));
                }
            }
            optResults.put(pendingScenario, oResult);

        }

        return optResults;
    }

    public void reportSummaryTo(PrintStream out) {

        Map<Scenario, Optional<Result>> ar = getAsyncResultStatus();

        for (Map.Entry<Scenario, Optional<Result>> entry : ar.entrySet()) {
            Scenario scenario = entry.getKey();
            Optional<Result> oresult = entry.getValue();

            out.print("scenario: " + scenario);

            if (oresult.isPresent()) {
                oresult.get().reportTo(out);
            } else {
                out.println(": incomplete");
            }
        }
    }
}
