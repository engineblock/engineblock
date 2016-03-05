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

package com.metawiring.load.activitytypes;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.StatementDef;
import com.metawiring.load.core.*;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This is the runtime shared object between all instances of a given YamlConfigurableActivity. For now, it is CQL-flavored.
 */
public class CQLActivityContext {
    private final static Logger logger = LoggerFactory.getLogger(CQLActivityContext.class);

    Timer timerOps;
    Timer timerWaits;
    Counter activityAsyncPendingCounter;
    Histogram triesHistogram;
    ReadyStatementsTemplate readyStatementsTemplate;
    ActivityDef activityDef;
    ScopedCachingGeneratorSource activityScopedGeneratorSource;
    Session session;

    public CQLActivityContext(ActivityDef def, ScopedCachingGeneratorSource scopedCachingGeneratorSource) {
        this.session = createSession(def);
        timerOps = MetricsContext.metrics().timer(name(def.getAlias(), "ops-total"));
        timerWaits = MetricsContext.metrics().timer(name(def.getAlias(), "ops-wait"));
        activityAsyncPendingCounter = MetricsContext.metrics().counter(name(def.getAlias(), "async-pending"));
        triesHistogram = MetricsContext.metrics().histogram(name(def.getAlias(), "tries-histogram"));
        MetricsContext.metrics().meter(name(def.getAlias(), "exceptions", "PlaceHolderException"));
        this.activityScopedGeneratorSource = scopedCachingGeneratorSource;
    }

    private Session createSession(ActivityDef def) {
        String host = def.getParams().getStringOrDefault("host","localhost");
        int port = def.getParams().getIntOrDefault("port",9042);
        Cluster.Builder builder = Cluster.builder()
                .addContactPoint(host)
                .withPort(port)
                .withCompression(ProtocolOptions.Compression.NONE);

        Optional<String> user = def.getParams().getOptionalString("user");
        Optional<String> pass = def.getParams().getOptionalString("password");
        if (user.isPresent() && pass.isPresent()) {
            builder.withCredentials(user.get(),pass.get());
        }

        Session session = builder.build().newSession();

        System.out.println("cluster-metadata-allhosts:\n" + session.getCluster().getMetadata().getAllHosts());
        return session;
    }

    public ReadyStatementsTemplate initReadyStatementsTemplate(List<StatementDef> statementDefs) {

        readyStatementsTemplate = new ReadyStatementsTemplate(
                session,
                activityScopedGeneratorSource,
                activityDef.getParams()
        );

        readyStatementsTemplate.addStatementDefs(statementDefs);
        readyStatementsTemplate.prepareAll();
        return readyStatementsTemplate;

    }

    public ReadyStatementsTemplate getReadyStatementsTemplate() {

        if (readyStatementsTemplate == null) {
            throw new RuntimeException("The ready statements template must be prepared with the initReadyStatementsTemplate(StatementDef...) method");
        }

        return readyStatementsTemplate;
    }

    public void createSchema(List<StatementDef> statementDefs) {
        ReadyStatementsTemplate readyStatementsTemplate = new ReadyStatementsTemplate(
                session,
                activityScopedGeneratorSource,
                this.activityDef.getParams()
        );
        readyStatementsTemplate.addStatementDefs(statementDefs);
        readyStatementsTemplate.prepareAll();
        ReadyStatements rs = readyStatementsTemplate.bindAllGenerators(0);
        for (ReadyStatement readyStatement : rs.getReadyStatements()) {
            BoundStatement bound = readyStatement.bind();
            session.execute(bound);
        }


    }

}
