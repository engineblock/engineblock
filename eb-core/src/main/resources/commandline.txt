Basic Usage:

Discovery options:

  These options help you learn more about running PROG, and
  about the plugins that are present in your particular version.

  --help
      Basic help ( You're looking at it. )
  --help <activity type>
      Provide specific help for the named activity type
  --advanced-help
      Provide advanced help that explains more than this screen.
  --list-activity-types
      List the available activity types
  --list-metrics <activity type> [ <activity name> ]
      Provide the metrics that are available for scripting

Execution Options:

  This is how you actually tell PROG what scenario to run. Each of these
  commands appends script logic to the scenario that will be executed.
  These are considered as commands, can occur in any order and quantity.
  The only rule is that arguments in the arg=value form will apply to
  the preceding script or activity.

  script <script file> [arg=value]...
      Add the named script file to the scenario, interpolating named parameters

  activity [arg=value]...
      Add the named activity to the scenario, interpolating named parameters

General options:

  These options modify how the scenario is run.

  --report-graphite-to <addr>[:<port>]
      Specify the graphite destination and enable reporting

  --metrics-prefix <metrics-prefix>
      Specify the metrics name prefix for graphite reporting

  --session-name <name>
      Name the current session, for logfile naming, etc
      By default, this will be "scenario-TIMESTAMP", and a logfile will be created
      for this name.

Options:
  -h                               Show help.
  -v | --verbose                   Report internal session log to console at info level
  -vv | --veryverbose              Report internal session log to console, at trace level
  -V | --version                   Show version, long form, with artifact coordinates.




