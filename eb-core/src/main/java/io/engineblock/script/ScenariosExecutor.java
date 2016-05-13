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

import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ScenariosExecutor {

    private final static Logger logger = LoggerFactory.getLogger(ScenariosExecutor.class);

    private LinkedHashMap<String, Future<Result>> submittedScenarios = new LinkedHashMap<String, Future<Result>>();
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    public ScenariosExecutor() {
    }

    public ScenariosExecutor(int threads) {
        executor = Executors.newFixedThreadPool(threads);
    }

    public void execute(Scenario scenario) {
        Future<Result> submitted = executor.submit(scenario);
        submittedScenarios.put(scenario.getName(), submitted);
    }

    public Map<String, Result> awaitAllResults() {
        return awaitAllResults(Long.MAX_VALUE/2, 60000); // half max value, to avoid overflow
    }

    public Map<String, Result> awaitAllResults(long timeout, long updateInterval) {
        if (updateInterval > timeout) {
            throw new InvalidParameterException("timeout must be equal to or greater than updateInterval");
        }
        long timeoutAt = System.currentTimeMillis() + timeout;

        boolean terminated = false;
        while (!terminated && System.currentTimeMillis() < timeoutAt) {
            long updateAt = Math.min(timeoutAt, System.currentTimeMillis() + updateInterval);
            while (!terminated && System.currentTimeMillis() < timeoutAt) {

                while (!terminated && System.currentTimeMillis() < updateAt) {
                    try {
                        long timeRemaining = timeoutAt - System.currentTimeMillis();
                        logger.debug("Waiting for timeRemaining:" + timeRemaining + "ms for scenarios executor to shutdown.");
                        terminated = executor.awaitTermination(timeRemaining, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                    }
                }
                updateAt = Math.min(timeoutAt, System.currentTimeMillis() + updateInterval);
            }

            logger.info("waited " + updateInterval + "ms for executor termination so far. terminated: "
                    + terminated + " isTerminated" + executor.isTerminated() + " isShutdown:" + executor.isShutdown());
        }

        if (!terminated) {
            throw new RuntimeException("executor still runningScenarios after awaiting all results for " + timeout
                    + "ms.  isTerminated:" + executor.isTerminated() + " isShutdown:" + executor.isShutdown());
        }
        Map<String, Result> results = new LinkedHashMap<>();
        getAsyncResultStatus()
                .entrySet().stream()
                .forEach(es -> results.put(es.getKey(), es.getValue().orElseGet(null)));
        return results;
    }

    /**
     * @return list of scenarios which have been submitted, in order
     */
    public List<String> getPendingScenarios() {
        return new ArrayList<String>(submittedScenarios.keySet());
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
    public Map<String, Optional<Result>> getAsyncResultStatus() {
        Map<String, Optional<Result>> optResults = new LinkedHashMap<>();
        for (String pendingName : submittedScenarios.keySet()) {
            Future<Result> resultFuture = submittedScenarios.get(pendingName);

            Optional<Result> oResult = Optional.empty();
            if (resultFuture.isDone()) {
                try {
                    oResult = Optional.of(resultFuture.get());
                } catch (Exception e) {
                    oResult = Optional.of(new Result(e));
                }
            }
            optResults.put(pendingName, oResult);

        }

        return optResults;
    }
}
