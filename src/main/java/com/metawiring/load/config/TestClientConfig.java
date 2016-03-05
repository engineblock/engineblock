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

package com.metawiring.load.config;

import com.datastax.driver.core.ConsistencyLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything in the config is
 */
public class TestClientConfig {

    // Making these fields mutable for now to experiment with Nashorn usage patterns

    public String host;
    public int port;
    public List<ActivityDef> activities;
    public String graphiteHost,metricsPrefix;
    public int graphitePort;
    public String keyspace;
    public String table;
    public boolean createSchema;
    public String user;
    public String password;
    public ConsistencyLevel defaultConsistencyLevel;
    public int defaultReplicationFactor;
    public boolean splitCycles;
    public boolean diagnoseExceptions;

    public static Builder builder() {
        return new Builder();
    }

    /**
     * You probably want to use the static builder() method instead of calling this onerous constructor.
     */
    public TestClientConfig(
            String host, int port,
            List<ActivityDef> activityDefs,
            String metricsPrefix,
            String graphiteHost,
            int graphitePort,
            int verbosity,
            String keyspace,
            String table,
            boolean createSchema,
            String user,
            String password,
            int defaultReplicationFactor,
            ConsistencyLevel defaultConsistencyLevel,
            boolean splitCycles,
            boolean diagnoseExceptions) {
        this.host = host;
        this.port = port;
        this.activities = new ArrayList<>(activityDefs);
        this.metricsPrefix = metricsPrefix;
        this.graphiteHost = graphiteHost;
        this.graphitePort = graphitePort;
        this.keyspace = keyspace;
        this.table = table;
        this.createSchema = createSchema;
        this.user = user;
        this.password = password;
        this.defaultConsistencyLevel = defaultConsistencyLevel;
        this.defaultReplicationFactor = defaultReplicationFactor;
        this.splitCycles = splitCycles;
        this.diagnoseExceptions = diagnoseExceptions;
    }


    public static class Builder {

        private String host = "localhost";
        private int port = 9042;
        private List<ActivityDef> activityDefs = new ArrayList<>();
        private String metricsPrefix="";
        private String graphiteHost;
        private int graphitePort;
        private int verbosity=0;
        private String keyspace="test";
        private String table="test";
        private boolean createSchema=false;
        private String user;
        private String password;
        private ConsistencyLevel defaultConsistencyLevel= ConsistencyLevel.ONE;
        private int defaultReplicationFactor = 1;
        private boolean splitCycles = true;
        private boolean diagnoseExceptions = false;
        private int replicationFactor = 1;

        public TestClientConfig build() {
            return new TestClientConfig(
                    host,
                    port,
                    new ArrayList<>(activityDefs),
                    metricsPrefix,
                    graphiteHost,
                    graphitePort,
                    verbosity,
                    keyspace,
                    table,
                    createSchema,
                    user,
                    password,
                    defaultReplicationFactor,
                    defaultConsistencyLevel,
                    splitCycles,
                    diagnoseExceptions);
        }

        public Builder withCredentials(String user, String password) {
            this.user = user;
            this.password = password;
            return this;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withSplitCycles(boolean splitCycles) {
            this.splitCycles = splitCycles;
            return this;
        }

        public Builder withDiagnoseExceptions(boolean diagnoseExceptions) {
            this.diagnoseExceptions = diagnoseExceptions;
            return this;
        }

        /**
         * @param activity - activity name in one of the formats:
         * <UL>
         *                 <LI>activityClass</LI>
         *                 <LI>activityClass:cycles</LI>
         *                 <LI>activityClass:cycles:threads</LI>
         *                 <LI>activityClass:cycles:threads:maxAsync/LI>
         * </UL>
         *                 where cycles may be either M or N..M
         *                 N implicitly represent 1..M
         * @return builder
         */
        public Builder addActivityDef(String activity) {
            String[] parts = activity.split(":");
            String aName;
            String aThreads="1";
            String aCycles="1";
            String aMaxSync="1";
            String aInterCycleDelay="0";

            int interCycleDelay = 0;
            switch (parts.length) {
                case 5: aInterCycleDelay = parts[4];
                case 4: aMaxSync = parts[3];
                case 3: aThreads = parts[2];
                case 2: aCycles = parts[1];
                case 1: aName = parts[0];
                    break;
                default:
                    throw new RuntimeException("Invalid activity definition: " + activity);
            }
            String[] cycleParts = aCycles.split("\\.\\.");
            String aCyclesMin, aCyclesMax;
            switch (cycleParts.length) {
                case 2:
                    aCyclesMin=cycleParts[0];
                    aCyclesMax=cycleParts[1];
                    break;
                case 1:
                    aCyclesMin="0";
                    aCyclesMax=cycleParts[0];
                    break;
                default:
                    throw new RuntimeException("Invalid cycles definitions: " + aCycles);
            }
            activityDefs.add(
                    ActivityDef.parseActivityDef(
                            "name="+aName+";"
                            + "mincycle=" + aCyclesMin + ";"
                            + "maxcycle=" + aCyclesMax + ";"
                            + "threads=" + aThreads + ";"
                            + "async=" + aMaxSync + ";"
                            + "delay=" + aInterCycleDelay + ";"
                    )
            );

            return this;
        }

        public Builder withActivityDefs(List<String> strings) {
            activityDefs.clear();
            for (String string : strings) {
                addActivityDef(string);
            }
            return this;
        }

        public Builder withGraphite(String graphite) {
            this.graphiteHost = graphite.split(":")[0];
            this.graphitePort = graphite.contains(":") ? Integer.valueOf(graphite.split(":")[1]) : 2003;
            return this;
        }

        public Builder withMetricsPrefix(String metricsPrefix) {
            this.metricsPrefix = metricsPrefix;
            return this;
        }

        public Builder withKeyspace(String keyspace) {
            this.keyspace = keyspace;
            return this;
        }

        public Builder withTable(String table) {
            this.table =table;
            return this;
        }

        public Builder withCreateSchema(Boolean createSchema) {
            this.createSchema = createSchema;
            return this;
        }

        public Builder withDefaultConsistencyLevel(String cl) {
            this.defaultConsistencyLevel= ConsistencyLevel.valueOf(cl);
            return this;
        }

        public Builder withDefaultReplicationFactor(String rf) {
            this.defaultReplicationFactor=Integer.valueOf(rf);
            return this;
        }

    }
}
