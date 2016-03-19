### Activities

The contextual workloads are defined as _Activities_, which is just an interface that an ActivityHarness uses to run the activity. You can specify how
the iterations are divided up between the threads. By default, the specific cycle numbers will not be assigned distinctly to the threads, although the
cycle counts will. If you want the cycles to be divided up by range, then use the --splitcycles option. This applies to all activities on the command line for now.

You have the option of using one of the direct Activity types or a yaml configured activity. The preferred way is to use YAML to configure and run your activities, since the internal boilerplate logic is pretty standard. The previous activity implementations were left as examples for those who might want to tinker with or build their own activity implementations.

The remainder of the documentation describes all of the current activities in general. If you implement your own activity, then it may not apply, depending on how you choose to build it.

#### Cycle Semantics

Each activity is run by an executor service under the control of an ActivityHarness for each thread.
Each time an activity harness iterates an activity, it expects the activity to have completed one cycle of work. This should be considered the sementic contract for an Activity. It allows results to be interpreted across activities more trivially.

#### Fault Handling

Some details about how async activities work:
- Before any inner loop is called, the activity is initialized, including prepared statements and data generator bindings.
- The inner loop always tries to fill the async pipeline up to the configured allowance.
- After the async pipeline is primed, the second phase of the inner loop does the following:
 - tries to get the async result
 - if this fails before the 10th try, then the op is resubmitted and the thread sleeps for 0.1 * tries seconds
 - if the op failed and at least 10 tries have been used, then the op is not retried

##### activity: WriteTelemetryAsync AKA write-telemetry

The schema for this activity is

    CREATE TABLE testtable (
      source int,
      epoch_hour text,
      param text,
      ts timestamp,
      cycle bigint,
      data text,
      PRIMARY KEY ((source, epoch_hour), param, ts)
    ) WITH CLUSTERING ORDER BY (param ASC, ts DESC)

CQL for this activity:

    insert into KEYSPACE.TABLE (source,epoch_hour,param,ts,data,cycle) values (?,?,?,?,?,?)

where the fields are, respectively:

- __source__ - The thread id (This simulates a topological or taxonomical source id)
- __epoch_hour__ - A time bucket, in the form _"2005-10-29-22"_, walking forward from the epoch start, advancing 1 second each time it is used for each thread.
- __param__ - A randomly selected parameter name, from a list of 100 selected at random from the 'net
- __ts__ - A timestamp, generated forward in time, which coincides with the epoch_hour bucket, advancing at the same rate.
- __cycle__ - The cycle number of the activity thread that created this row
- __data__ - a random extract of the full lorem ipsum text, randome size between 100-200 characters

As this activity runs, it creates data that moves forward in time, starting at the beginning of the epoch. This is suitable to DTCS testing and general time-series or temporally-ordered testing. If you want to control the number of rows written, overall, then the cycle count in the activity option does this. If you want to control the specific times that are used, then the cycle range in (min..max] format can do this. However, the math is thrown off if you change the number of threads, since the cycles are distributed among all threads, while the starting cycle set on all of them.

##### activity: ReadTelemetryAsync AKA read-telemetry

This activity uses the same schema as WriteTelemetryAsync. 

CQL for this activity:

    select * from KEYSPACE.TABLE where source=? and epoch_hour=? and param=? limit 10

where the fields are, respectively:

- __source__ - same semantics and behavior as above
- __epoch_hour__ - same semantics and behavior as above
- __param__ - same semantics and behavior as above

This means that reads will be mostly constrained by partition, which is good. However, the logic doesn't automatically walk backwards in time to previous epoch_hour buckets to get a full 10 items. This compromise was made until the testing tool internals can be refined to support such cases without overcomplicating things. (A specific plan is in the works.)

#### Generators and Thread affinity

Internally, the data that is used in the operations is produced by type-parameterized generators. This means that if you want a second-resolution DateTime object, then you have to have a generator of type Generator&lt;DateTime&gt; with the implementation and instance details to handle the second resolution.

The generator library handles these details as well as when generator instances are shared between activity threads. There is a special type of generator, ThreadNumGenerator, which uses markers on the thread to extract thread enumeration. This is used by the two initial activities above as a way to make each thread align to a partition. This isn't required, but for the type of testing that this tool was built for, it effectively guaranteed isochronous data rates evenly across the partitions. The point of calling this out here is to acknowledge that your testing might not need this, and would benefit from wider data dispersion at the partition specificity. There is nothing preventing such use-- It merely isn't the default for these activities.

#### Extending cqltestclient

If you need to build a test client for a particular workload, you might need to add to the generator library. The generators can be browsed in the source tree.

##### Generator Conventions

If you are going to add generators, follow these guidlines:

Generators constructors which take parameters should provide a constructor which uses all String arguments at the very least. Generators which are threadsafe should implement ThreadSafeGenerator, and those which can be advanced to a particular point in the cycle count should also implement FastForwardableGenerator.

ThreadSafeGenerator is simply a tagging interface which allows the generator resolver to avoid sharing generators which are not thread-safe.

FastForwardableGenerator is an interface that allows an activity to advance the starting point for a generator so that you can control the range of cycles used in your test.

#### YAML Activity Configuration

Here is an example activity as configured in YAML:

    ddl:
    - name: create-keyspace
      cql: |
        create keyspace if not exists <<KEYSPACE>> WITH replication =
        {'class': 'SimpleStrategy', 'replication_factor': <<RF>>};
    - name: create-telemetry-table
      cql: |
        create table if not exists <<KEYSPACE>>.<<TABLE>>_telemetry (
        source int,      // data source id
        epoch_hour text, // time bucketing
        param text,      // variable name for a type of measurement
        ts timestamp,    // timestamp of measurement
        cycle bigint,    // cycle, for diagnostics
        data text,       // measurement data
        PRIMARY KEY ((source, epoch_hour), param, ts)
        ) WITH CLUSTERING ORDER BY (param ASC, ts DESC)
    dml:
     - name: write-telemetry
       cql: |
         insert into <<KEYSPACE>>.<<TABLE>> (source, epoch_hour, param, ts, data, cycle)
         values (<<source>>,<<epoch_hour>>,<<param>>,<<ts>>,<<data>>,<<cycle>>);
       bindings:
         source: threadnum
         epoch_hour: date-epoch-hour
         param: varnames
         ts: datesecond
         data: loremipsum:100:200
         cycle: cycle

The __ddl__ section contains the statements needed to configure your keyspace and tables. They will get called in order when you are executing cqltestclient with --createSchema.
The __dml__ section contains the statements that you want to run for each cycle. Just as with the DDL statements, you can have as many as you like. The activity will use one of these for each cycle, rotating through them in order.

The format of the __cql__ section shows how to use multi-line statements in YAML while preserving newlines. As long as you preserve newlines, you're free to use // comments to explain the parameters. If you do not maintain the newlines, then you will have syntax errors because of invalid comments.

The __bindings__ sections are how named place-holders are mapped to data generator functions. Currently, these names are not cross-checked between the cql and binding names.

The bindings map a field position to a generator function. They must be in order of the target field names as specified in the cql. When it is time to instantiate a generator to feed values to a statement, the resolution can happen in one of two ways. First,  if the GeneratorSourceImpl class in the com.metawiring.load.generator package contains a mapping for the named generator, then that generator is used. Otherwise, if there is a class of the same name as the generator in the com.metawiring.load.generators package, it is instantiated and used.

The &lt;&lt;word&gt;&gt; convention is used for parameter substitution. KEYSPACE, TABLE, and RF are all substituted automatically from the command line options. The _create table_ clause above shows a convention that uses both the configured TABLE name as well as a _tablename value. This is a useful way to have a common configurable prefix when you are using multiple tables.

Both the __ddl__ and __dml__ sections contain exactly the same thing structurally. In fact, it's exactly the same configuration type internally. Both contain a list of named statements with their cql template and a set of associated bindings. You don't see any bindings under ddl because they are meaningless there for this example activity.
