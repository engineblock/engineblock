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

package com.metawiring.load.cli;

import com.metawiring.load.config.TestClientConfig;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestClientCLIOptions {
    private final static Logger logger = LoggerFactory.getLogger(TestClientCLIOptions.class);

    OptionParser parser;
    OptionSpec<Integer> port;
    OptionSpec<String> host;
    OptionSpec<Boolean> help;
    OptionSpec<String> activities;
    OptionSpec<Integer> maxAsync;
    OptionSpec<String> graphite;
    OptionSpec<String> metricsPrefix;
    OptionSpec<String> keyspace;
    OptionSpec<String> table;
    OptionSpec<Boolean> createSchema;
    OptionSpec<String> user;
    OptionSpec<String> password;
    OptionSpec<String> defaultRF;
    OptionSpec<String> defaultCL;
    OptionSpec<Boolean> splitCycles;
    OptionSpec<Boolean> diagnoseExceptions;

    {
        parser = new OptionParser();

        host = parser.accepts("host").withRequiredArg().ofType(String.class)
                .defaultsTo("localhost")
                .describedAs("a valid host in the target cluster");

        port = parser.accepts("port").withRequiredArg().ofType(Integer.class)
                .defaultsTo(9042)
                .describedAs("The port of the native driver, usually 9042");

        help = parser.accepts("help").withOptionalArg().ofType(Boolean.class)
                .defaultsTo(false)
                .describedAs("Get some help.");

        graphite = parser.accepts("graphite").withRequiredArg().ofType(String.class)
                .describedAs("The host and port in host or host:port form for graphite reporting");

        // Try to getMotor the client name in client-A-B-C-D form, or just leave it as client-unknownaddr
        String defaultClientName = "client-unknownaddr";
        try {
            defaultClientName = "client-"+ InetAddress.getLocalHost().getHostAddress().replaceAll("\\.","-");
        } catch (UnknownHostException e) {
            logger.warn("Unable to determine a useful client name, use --prefix to set one.");
            logger.warn(e.getMessage());

        }

        metricsPrefix = parser.accepts("prefix").withRequiredArg().ofType(String.class)
                .defaultsTo(defaultClientName)
                .describedAs("The base prefix to append to all metrics reporter names");

        activities = parser.accepts("activity").withRequiredArg().ofType(String.class)
                .required()
                .describedAs("Activities to run, format: --activity=name:cycles:threads:asyncs");

        keyspace = parser.accepts("keyspace").withRequiredArg().ofType(String.class)
                .describedAs("Keyspace to use").defaultsTo("testks");

        table = parser.accepts("table").withRequiredArg().ofType(String.class)
                .describedAs("Table to use").defaultsTo("testtable");

        createSchema = parser.accepts("createschema").withOptionalArg().ofType(Boolean.class)
                .describedAs("create schemas and exit").defaultsTo(false);

        user = parser.accepts("user").withRequiredArg().ofType(String.class)
                .describedAs("user to connect as").defaultsTo("");

        password = parser.accepts("password").withRequiredArg().ofType(String.class)
                .describedAs("password to authenticate with").defaultsTo("");


        defaultCL = parser.accepts("cl").withRequiredArg().ofType(String.class)
                .describedAs("default consistency level for each operation").defaultsTo("ONE");

        defaultRF = parser.accepts("rf").withRequiredArg().ofType(String.class)
                .describedAs("replication factor to use when creating schema").defaultsTo("1");

        splitCycles = parser.accepts("splitcycles").withOptionalArg().ofType(Boolean.class)
                .describedAs("divide cycle ranges among threads").defaultsTo(false);

        diagnoseExceptions = parser.accepts("diagnose").withOptionalArg().ofType(Boolean.class)
                .describedAs("causes verbose exceptions to be thrown instead of retried quietly.").defaultsTo(false);
    }

    public TestClientConfig parse(String[] args) {

        OptionSet options=null;

        try {
            options = parser.parse(args);
        } catch (Exception e) {
            try {
                System.out.println("ERROR: " + e.getMessage());
                parser.printHelpOn(System.out);
            } catch (IOException ignored) {
            }
            System.exit(2);
        }

        if (options.has(help)) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(2);
        }

        TestClientConfig.Builder builder = TestClientConfig.builder();
        builder.withHost(options.valueOf(host));
        builder.withPort(options.valueOf(port));
        builder.withActivityDefs(options.valuesOf(activities));
        builder.withMetricsPrefix(options.valueOf(metricsPrefix));
        builder.withKeyspace(options.valueOf(keyspace));
        builder.withTable(options.valueOf(table));
        builder.withCreateSchema(options.has(createSchema));
        builder.withCredentials(options.valueOf(user),options.valueOf(password));
        builder.withDefaultConsistencyLevel(options.valueOf(defaultCL));
        builder.withDefaultReplicationFactor(options.valueOf(defaultRF));
        builder.withSplitCycles(options.has(splitCycles));
        builder.withDiagnoseExceptions(options.has(diagnoseExceptions));

        if (options.has(graphite)) {
            builder.withGraphite(options.valueOf(graphite));
        }

        TestClientConfig config = builder.build();
        return config;
    }
}
