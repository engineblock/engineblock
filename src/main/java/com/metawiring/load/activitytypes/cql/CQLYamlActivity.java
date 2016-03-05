/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.activitytypes.cql;

import static com.codahale.metrics.MetricRegistry.name;

public class CQLYamlActivity {

//    private static Logger logger = LoggerFactory.getLogger(CQLYamlActivity.class);
//
//    private long endCycle, submittedCycle;
//    private int pendingRq = 0;
//    private long maxAsync = 0l;
//
//    private ReadyStatements readyStatements;
//    private LinkedList<TimedFuture<ResultSetFuture>> timedFutures = new LinkedList<>();
//    private CQLYamlActivityContext activityContext;
//    private YamlActivityDef yamlActivityDef;
//
//    public CQLYamlActivity(YamlActivityDef yamlActivityDef) {
//        this.yamlActivityDef = yamlActivityDef;
//    }
//
//    @Override
//    public void prepare(long startCycle, long endCycle, long maxAsync) {
//
//        this.maxAsync = maxAsync;
//        this.endCycle = endCycle;
//        submittedCycle = startCycle - 1L;
//
//        readyStatements = activityContext.getReadyStatementsTemplate().bindAllGenerators(startCycle);
//    }
//
//    public void createSchema() {
//
//        activityContext.createSchema();
//
////        for (YamlActivityDef.StatementDef statementDef : yamlActivityDef.getDdl()) {
////            String qualifiedStatement = statementDef.getCookedStatement(context.getConfig());
////            try {
////                logger.info("Executing DDL statement:\n" + qualifiedStatement);
////                session.execute(qualifiedStatement);
////                logger.info("Executed DDL statement [" + qualifiedStatement + "]");
////            } catch (Exception e) {
////                logger.error("Error while executing statement [" + qualifiedStatement + "] " + context.getConfig().keyspace, e);
////                throw new RuntimeException(e); // Let this escape, it's a critical runtime exception
////            }
////        }
////
//    }
//
//    /**
//     * Normalize receiving rate to 1/iterate() for now, with bias on priming rq queue
//     */
//    @Override
//    public void iterate() {
//
//        // Not at limit, let the good times roll
//        // This section fills the async pipeline to the configured limit
//        while ((submittedCycle < endCycle) && (pendingRq < maxAsync)) {
//            long submittingCycle = submittedCycle + 1;
//            try {
//
//                TimedFuture trsf = new TimedFuture<ResultSetFuture>();
//                ReadyStatement nextStatement = readyStatements.getNext(submittingCycle);
//
//                trsf.boundStatement = nextStatement.bind();
//
//                trsf.timerContext = activityContext.timerOps.time();
//                trsf.future = activityContext.session.executeAsync(trsf.boundStatement);
//                trsf.tries++;
//
//                timedFutures.add(trsf);
//                activityContext.activityAsyncPendingCounter.inc();
//                pendingRq++;
//                submittedCycle++;
//
//            } catch (Exception e) {
//                instrumentException(e);
//            }
//        }
//
//        // This section attempts to process one async response per iteration. It always removes one from the queue,
//        // and if necessary, resubmits and waits synchronously for retries. (up to 9 more times)
//        int triesLimit = 10;
//
//
//        TimedFuture<ResultSetFuture> trsf = timedFutures.pollFirst();
//        if (trsf == null) {
//            throw new RuntimeException("There was not a waiting future. This should never happen.");
//        }
//
//        while (trsf.tries < triesLimit) {
//            Timer.Context waitTimer = null;
//            try {
//                waitTimer = activityContext.timerWaits.time();
//                ResultSet resultSet = trsf.future.getUninterruptibly();
//                waitTimer.stop();
//                waitTimer = null;
//                break;
//            } catch (Exception e) {
//                if (waitTimer != null) {
//                    waitTimer.stop();
//                }
//                instrumentException(e);
//                trsf.future = activityContext.session.executeAsync(trsf.boundStatement);
//                try {
//                    Thread.sleep(trsf.tries * 100l);
//                } catch (InterruptedException ignored) {
//                }
//                trsf.tries++;
//            }
//        }
//
//        pendingRq--;
//        activityContext.activityAsyncPendingCounter.dec();
//        trsf.timerContext.stop();
//        activityContext.triesHistogram.update(trsf.tries);
//
//    }
//
//    public CQLYamlActivityContext createContextToShare(ActivityDef def, ScopedCachingGeneratorSource genSource, OldExecutionContext executionContext) {
//        CQLYamlActivityContext activityContext = new CQLYamlActivityContext(def, yamlActivityDef, genSource);
//        return activityContext;
//    }
//
//    public void loadSharedContext(CQLYamlActivityContext sharedContext) {
//        this.activityContext = sharedContext;
//    }
//
//    public Class<?> getSharedContextClass() {
//        return CQLYamlActivityContext.class;
//    }
//
//    protected void instrumentException(Exception e) {
//        String exceptionType = e.getClass().getSimpleName();
//        MetricsContext.metrics().meter(name(activityContext.activityDef.getAlias(), "exceptions", exceptionType)).mark();
//
//        if (activityContext.activityDef.getParams().getBoolOrDefault("throw",false)) {
//            throw new RuntimeException(e);
//        }
//    }

}