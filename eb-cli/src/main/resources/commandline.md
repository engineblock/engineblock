### Basic Command-Line Options ###

Help ( You're looking at it. )

    --help

Short options, like '-v' represent simple options, like verbosity.
Using multiples increases the level of the option, like '-vvv'.

Long options, like '--help' are top-level options that may only be
used once. These modify general behavior, or allow you to get more
details on how to use PROG.

All other options are either commands, or named arguments to commands.
Any single word without dashes is a command that will be converted
into script form. Any option that includes an equals sign is a
named argument to the previous command. The following example
is a commandline with a command *start*, and two named arguments
to that command.

    PROG start type=diag alias=example    

### Discovery options ###

These options help you learn more about running PROG, and
about the plugins that are present in your particular version.

Get a list of additional help topics that have more detailed
documentation:

    PROG help topics

Provide specific help for the named activity type:

    PROG help <activity type>

List the available activity types

    --list-activity-types

Provide the metrics that are available for scripting

    --list-metrics <activity type> [ <activity name> ]

### Execution Options ###

This is how you actually tell PROG what scenario to run. Each of these
commands appends script logic to the scenario that will be executed.
These are considered as commands, can occur in any order and quantity.
The only rule is that arguments in the arg=value form will apply to
the preceding script or activity.

Add the named script file to the scenario, interpolating named parameters:

    script <script file> [arg=value]...

Add the named activity to the scenario, interpolating named parameters

    activity [arg=value]...

### General options ###

These options modify how the scenario is run.

Specify a directory for scenario log files:

    --log-dir <dirname>

Specify a limit on logfiles (old files will be purged)

    --log-max <count>

Specify the logging pattern:

    --with-logging-pattern '%date %level [%thread] %logger{10} [%file:%line] %msg%n'

    ( default: '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n' )
    ( See https://logback.qos.ch/manual/layouts.html#ClassicPatternLayout for format options )

Specify a directory and enable CSV reporting of metrics:

    --report-csv-to <dirname>
    
Specify the graphite destination and enable reporting

    --report-graphite-to <addr>[:<port>]

Specify the interval for graphite or CSV reporting in seconds (default: 10)

    --report-interval <interval-seconds>

Specify the metrics name prefix for graphite reporting

    --metrics-prefix <metrics-prefix>

Log all HDR histogram data to a file

    --log-histograms histodata.log
    
Log HDR histogram stats to a CSV file

    --log-histostats stats.csv
    
Adjust the progress reporting indicatorSpec

    --progress console:10s
    
or

    --progress logonly:5m


Name the current session, for logfile naming, etc
By default, this will be "scenario-TIMESTAMP", and a logfile will be created
for this name.

    --session-name <name>

### Console Options ###
Increase console logging levels: (Default console logging level is *warning*)

    -v         (info)         
    -vv        (debug)
    -vvv       (trace)
    
    --progress console:1m (disables itself if -v options are used)
    
Show version, long form, with artifact coordinates.

    --version
