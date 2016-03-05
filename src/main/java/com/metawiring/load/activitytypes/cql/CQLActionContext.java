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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.YamlActivityDef;
import com.metawiring.load.core.MetricsContext;
import com.metawiring.load.core.ReadyStatementsTemplate;
import com.metawiring.load.generator.GeneratorInstantiator;
import com.metawiring.load.generator.RuntimeScope;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import com.metawiring.load.generator.ScopedGeneratorCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This is the runtime shared object between all instances of a given YamlConfigurableActivity. For now, it is CQL-flavored.
 */
@SuppressWarnings("Duplicates")
public class CQLActionContext {
    private final static Logger logger = LoggerFactory.getLogger(CQLActionContext.class);

    private static Session session;

    Timer timerOps;
    Timer timerWaits;
    Counter activityAsyncPendingCounter;
    Histogram triesHistogram;
    ReadyStatementsTemplate readyStatementsTemplate;
    YamlActivityDef yamlActivityDef;
    ActivityDef activityDef;

    ScopedCachingGeneratorSource activityScopedGeneratorSource =
            new ScopedGeneratorCache(new GeneratorInstantiator(), RuntimeScope.activity);

    public CQLActionContext(
            ActivityDef def,
            YamlActivityDef yamlActivityDef) {
        this.activityDef = def;
        timerOps = MetricsContext.metrics().timer(name(def.getAlias(), "ops-total"));
        timerWaits = MetricsContext.metrics().timer(name(def.getAlias(), "ops-wait"));
        activityAsyncPendingCounter = MetricsContext.metrics().counter(name(def.getAlias(), "async-pending"));
        triesHistogram = MetricsContext.metrics().histogram(name(def.getAlias(), "tries-histogram"));
        MetricsContext.metrics().meter(name(def.getAlias(), "exceptions", "PlaceHolderException"));
        this.yamlActivityDef = yamlActivityDef;
    }

    private ReadyStatementsTemplate prepareReadyStatementsTemplate() {

        Session session = getSession();

        ReadyStatementsTemplate readyStatementsTemplate = new ReadyStatementsTemplate(
                getSession(),
                activityScopedGeneratorSource,
                activityDef.getParams()
        );
        readyStatementsTemplate.addStatementsFromYaml(yamlActivityDef, "dml");
        readyStatementsTemplate.prepareAll();
        return readyStatementsTemplate;
    }

    public ReadyStatementsTemplate getReadyStatementsTemplate() {

        if (readyStatementsTemplate == null) {
            synchronized (this) {
                if (readyStatementsTemplate == null) {
                    readyStatementsTemplate = this.readyStatementsTemplate = prepareReadyStatementsTemplate();
                }
            }
        }

        return readyStatementsTemplate;
    }

//    public void createSchema() {
//        ReadyStatementsTemplate readyStatementsTemplate = new ReadyStatementsTemplate(
//                getSession(),
//                getActivityGeneratorSource(),
//                getActivityDef().getParams()
//        );
//        readyStatementsTemplate.addStatements(yamlActivityDef, "ddl");
//        readyStatementsTemplate.prepareAll();
//        ReadyStatements rs = readyStatementsTemplate.bindAllGenerators(0);
//        for (ReadyStatement readyStatement : rs.getReadyStatements()) {
//            BoundStatement bound = readyStatement.bind();
//            session.execute(bound);
//        }
//
//    }

    public Session getSession() {

        if (session == null) {
            synchronized (this) {
                if (session == null) {
                    Cluster.Builder builder = Cluster.builder()
                            .addContactPoint(activityDef.getParams().getStringOrDefault("host", "127.0.0.1"))
                            .withPort(activityDef.getParams().getIntOrDefault("port", 9042))
                            .withCompression(ProtocolOptions.Compression.NONE);

                    if (activityDef.getParams().getStringOrDefault("user", null) != null) {
                        builder.withCredentials(
                                activityDef.getParams().getStringOrDefault("user", null),
                                activityDef.getParams().getStringOrDefault("password", null)
                        );
                    }

                    Cluster cluster = builder.build();

                    session = cluster.newSession();
                    System.out.println("cluster-metadata-allhosts:\n" + session.getCluster().getMetadata().getAllHosts());
                }
            }
        }

        return session;
    }
}
