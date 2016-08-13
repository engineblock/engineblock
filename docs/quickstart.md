# Quick Start

## Installation

EngineBlock is released as an executable jar. To use a specific release, simply download it and run it like this:
~~~
curl -o eb-cli.jar https://repo1.maven.org/maven2/io/engineblock/eb-cli/1.0.17/eb-cli-1.0.17.jar
java -jar eb-cli.jar
~~~

The latest release can always be run with this simple script: [run-eb](https://raw.githubusercontent.com/engineblock/engineblock/master/eb-cli/bin/run-eb). You can use the commands below to get it. 

~~~
curl -O https://raw.githubusercontent.com/engineblock/engineblock/master/eb-cli/bin/run-eb
chmod u+x run-eb
~~~

**A cautionary word**: As always, blindly running scripts from the web is not a good idea. Look at this script before you run it. Make sure you are comfortable with what it does.

This script demonstrates how to fetch the version of the latest published artifact via maven central search. It also downloads that artifact if it doesn't find it locally cached.

This script may be used in place of:

~~~
java -jar eb.jar arg0 arg1 ...
~~~
All command line arguments will be carried through.

# Usage

__Available Activity Types__

~~~
./run-eb --list-activity-types
~~~

This should provide a list of the activity types that are known to the client. The build-in activity type "diag", is always available. To really be useful, you need to add activity type libraries to the libs directory.

__Activity Type Docs__

You can get further documentation about an activity...
~~~
./run-eb help diag
~~~

This will show the internal documentation for the diag activity type. There will be documentation for every activity, as the activity loader will not recognize one without it.

__Running an Activity__

You can run an instance of an activity:
~~~
./run-eb activity alias=test1 type=diag threads=10 interval=200
~~~

This shows the standard form of an activity definition. It is simply a map of activity parameters and values.
Here is what they do:

- __type__ - All activities must have a type. This determines which ActivityType implementation is used to run the activity.
- __alias__ - All dynamically controlled activities must have an alias. The activity alias is the only way to modify a running activity once it is started. Think of the alias as the handle to the activity instance.
- __threads__ - All activities have this parameter. If you do not specify it, it defaults to 1. This determines how many threads are run concurrently within the activity.

You can also run multiple activities:

~~~
./run-eb \
activity alias=test1 type=diag threads=10 interval=200  \
activity alias=test2 type=diag threads=1 interval=10
~~~

__Scripting__

~~~
./run-eb somescript
~~~


There are many other command line options available. For more advanced usage, 
consult the [Command Line Reference](command_line.md), or even the full
[Usage Guide](usage_guide.md)