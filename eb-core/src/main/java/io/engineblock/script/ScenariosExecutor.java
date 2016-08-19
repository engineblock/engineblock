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
import io.engineblock.core.ScenariosResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScenariosExecutor {

    private final static Logger logger = LoggerFactory.getLogger(ScenariosExecutor.class);

    private LinkedHashMap<String, SubmittedScenario> submitted = new LinkedHashMap<>();
//
//    private LinkedHashMap<String, Future<Result>> submittedFutures = new LinkedHashMap<>();
//    private LinkedHashMap<String, Scenario> submittedScenarios = new LinkedHashMap<>();

    private ExecutorService executor = Executors.newFixedThreadPool(1);
    private String name;

    public ScenariosExecutor(String name) {
        this.name = name;
    }

    public ScenariosExecutor(String name, int threads) {
        executor = Executors.newFixedThreadPool(threads);
        this.name = name;
    }

    public synchronized void execute(Scenario scenario) {
        if (submitted.get(scenario.getName())!=null) {
            throw new RuntimeException("Scenario " + scenario.getName() + " is already defined. Remove it first to reuse the name.");
        }
        Future<Result> future = executor.submit(scenario);
        SubmittedScenario s = new SubmittedScenario(scenario, future);
        submitted.put(s.getName(), s);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Shuts down all running scenarios and awaits all results.
     *
     * @return the final scenario-result map.
     */
    public ScenariosResults awaitAllResults() {
        return awaitAllResults(Long.MAX_VALUE / 2, 60000); // half max value, to avoid overflow
    }

    /**
     * Shuts down all running scenarios and awaits all results.
     *
     * @param timeout        how long to wait for the results to complete
     * @param updateInterval how frequently to log status while waiting
     * @return the final scenario-result map
     */
    public ScenariosResults awaitAllResults(long timeout, long updateInterval) {
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
                        logger.debug("Waiting for timeRemaining:" + timeRemaining + "ms for scenarios executor to shutdownActivity.");
                        isShutdown = executor.awaitTermination(timeRemaining, TimeUnit.MICROSECONDS);
                    } catch (InterruptedException ignored) {
                    }
                }
                updateAt = Math.min(timeoutAt, System.currentTimeMillis() + updateInterval);
            }

            logger.info("scenarios executor shutdownActivity after " + (System.currentTimeMillis() - waitedAt) + "ms.");
        }

        if (isShutdown) {

        } else {
            throw new RuntimeException("executor still runningScenarios after awaiting all results for " + timeout
                    + "ms.  isTerminated:" + executor.isTerminated() + " isShutdown:" + executor.isShutdown());
        }
        Map<Scenario, Result> scenarioResultMap = new LinkedHashMap<Scenario, Result>();
        getAsyncResultStatus()
                .entrySet().stream()
                .forEach(es -> scenarioResultMap.put(es.getKey(), es.getValue().orElseGet(null)));
        return new ScenariosResults(this,scenarioResultMap);
    }

    /**
     * @return list of scenarios which have been submitted, in order
     */
    public List<String> getPendingScenarios() {
        return new ArrayList<String>(
                submitted.values().stream()
                        .map(SubmittedScenario::getName)
                        .collect(Collectors.toCollection(ArrayList::new)));
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

        for (SubmittedScenario submittedScenario : submitted.values()) {
            Future<Result> resultFuture = submittedScenario.getResultFuture();

            Optional<Result> oResult = Optional.empty();
            if (resultFuture.isDone()) {
                try {
                    oResult = Optional.of(resultFuture.get());
                } catch (Exception e) {
                    oResult = Optional.of(new Result(e));
                }
            }

            optResults.put(submittedScenario.getScenario(),oResult);

        }

        return optResults;
    }

    public Optional<Scenario> getPendingScenario(String scenarioName) {
        return Optional.ofNullable(submitted.get(scenarioName)).map(SubmittedScenario::getScenario);
    }

    /**
     * Get the result of a pending or completed scenario. If the scenario has run to
     * completion, then the Optional will be present. If the scenario threw an
     * exception, or there was an error accessing the future, then the result will
     * contain the exception. If the callable for the scenario was cancelled, then the
     * result will contain an exception stating such.
     *
     * If the scenario is still pending, then the optional will be empty.
     *
     * @param scenarioName the scenario name of interest
     * @return an optional result
     */
    public Optional<Result> getPendingResult(String scenarioName) {

        Future<Result> resultFuture1 = submitted.get(scenarioName).resultFuture;
        if (resultFuture1==null) {
            throw new RuntimeException("Unknown scenario name:" + scenarioName);
        }
        if (resultFuture1.isDone()) {
            try {
                return Optional.ofNullable(resultFuture1.get());
            } catch (Exception e) {
                return Optional.of(new Result(e));
            }
        } else if (resultFuture1.isCancelled()) {
            return Optional.of(new Result(new Exception("result was cancelled.")));
        }
        return Optional.empty();
    }

    public synchronized void cancelScenario(String scenarioName) {
        Optional<Scenario> pendingScenario = getPendingScenario(scenarioName);
        Optional<Result> pendingResult = getPendingResult(scenarioName);
        if (pendingScenario.isPresent()) {
            pendingScenario.get().getScenarioController().forceStopScenario(0);
            submitted.remove(scenarioName);
            logger.info("cancelled scenario "+ scenarioName);
        } else {
            throw new RuntimeException("Unable to cancel scenario: " + scenarioName + ": not found");
        }
    }

    public String getName() {
        return name;
    }

    private static class SubmittedScenario {
        private Scenario scenario;
        private Future<Result> resultFuture;

        SubmittedScenario(Scenario scenario, Future<Result> resultFuture) {
            this.scenario = scenario;
            this.resultFuture = resultFuture;
        }

        public Scenario getScenario() {
            return scenario;
        }

        Future<Result> getResultFuture() {
            return resultFuture;
        }

        public String getName() {
            return scenario.getName();
        }
    }
}
