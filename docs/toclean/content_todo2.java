# Command Line

To see the options supported, simply

    java -jar cqltestclient.jar -help

The basics have been included here as well:

      --activity=<class name>:<cycles>:<threads>:<asyncs>
      --createschema
      --keyspace=<keyspace>                             (default: testks)
      --table=<table>                                   (default: testtable)
    ( If there are several tables in an activity, this will be used as a prefix)
      --splitcycles                                     (default: false)
    [ --host <contact point for cluster> ]              (default: localhost)
    [ --port <CQL native port> ]                        (default: 9042)
    [ --graphite <host> | --graphite <host>:<port> ]
    [ --prefix <telemetry naming prefix> ]

### Example Command Lines

__create the schema for the write-telemetry activity__

    java -jar cqltestclient.jar --host=10.10.10.10 --activity=write-telemetry --createschema

In the example above, cqltestclient looks for an activity definition in activities/write-telemetry.yaml, and then in the classpath under the same resource path. It then calls the createSchema(), using the ddl contained in that file.

__insert 1000000 records using 100 threads and 1000 pending async__

    java -jar cqltestclient.jar --host=10.10.10.10 \
    --activity=write-telemetry:1000000:100:1000

Notice that this example is slightly different. The --activity argument is not a proper class name. In this case, cqltestclient will look for a yaml file in the classpath under activities/write-telemetry.yaml. It will then initialize and run a YamlConfigurableActivity with it. The backslash is there to break the longer line up for readability.

__read the same records back, with 100 threads and 200 pending async__

    java -jar cqltestclient.jar --host=10.10.10.10 \
    --activity=read-telemetry:1000000:100:200

__do both at the same time__

    java -jar cqltestclient.jar --host=10.10.10.10 \
    --activity=write-telemetry:1000000:100:1000 \
    --activity=read-telemetry:1000000:100:1000

__do both at the same time, with an additional 3ms delay between the reads on each thread__

    java -jar cqltestclient.jar --host=10.10.10.10 \
    --activity=write-telemetry:1000000:100:1000 \
    --activity=read-telemetry:1000000:100:1000:3

