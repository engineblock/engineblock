# Quick Start

## Getting EngineBlock

The latest release can always be run with this simple script: [run-eb](https://raw.githubusercontent.com/engineblock/engineblock/master/eb-cli/bin/run-eb). You can use the commands below to get it. 

~~~
curl -O https://raw.githubusercontent.com/engineblock/engineblock/master/eb-cli/bin/run-eb
chmod u+x run-eb
./run-eb help
~~~

This script demonstrates how to fetch the version of the latest published artifact via maven central search. It also downloads that artifact if it doesn't find it locally cached within the last 2 days.

**A cautionary word**: As always, blindly running scripts from the web is not a good idea. Look at this script before you run it. Make sure you are comfortable with what it does. If you want, you can always download the jar directly:

~~~
curl -o eb-cli.jar https://repo1.maven.org/maven2/io/engineblock/eb-cli/1.0.17/eb-cli-1.0.17.jar
java -jar eb-cli.jar
~~~

The run-eb script may be used in place of:

~~~
java -jar eb.jar arg0 arg1 ...
~~~
All command line arguments will be carried through, as in:
~~~
./run-eb arg0 arg1 ...
~~~

## Using Built-in Docs

~~~
./run-eb --list-activity-types
~~~

This should provide a list of the activity types that are known to the client. The built-in activity type "diag", is always available. If you are learning how to use EngineBlock, you will be using the "diag" activity quite a bit. If you are using a tailored engineblock runtime, you will have other activity types available according to your distribution.

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

There are other parameters available to an activity, depending on the activity type. To discover the parameters
for a given activity type and what they mean, use the *help &lt;activitytype&gt;* form shown above. 

__Running Multiple Activities__

You can also run multiple activities, like this
~~~
./run-eb \
activity alias=test1 type=diag threads=10 interval=200  \
activity alias=test2 type=diag threads=1 interval=10
~~~

This shows a basic form of command-line scripting. The verb *activity* as actually a synonym for *start*, but it is more
readable when all you are doing is starting some activities. The activities in this scenario are run concurrently,
because each one is started asynchronously. If you wanted to run them one at a time, you could do this instead:

~~~
./run-eb \
run alias=test1 type=diag threads=10 interval=200  \
run alias=test2 type=diag threads=1 interval=10
~~~

The *run* command is synchronous. It waits for the activity named 'test1' to complete before starting 'test2'.

## CLI Scripting

Any time you invoke engineblock, you are asking it to run a scenario. Scenarios are Nashorn scripts, always.
If you wanted to see the script that resulted from a particular command line, you can always ask, with the
--show-script option.

~~~
./run-eb --show-script \
run alias=test1 type=diag threads=10 interval=200  \
run alias=test2 type=diag threads=1 interval=10
~~~
will show you this:
~~~
// Script
scenario.run("alias=test1;type=diag;threads=10;interval=200;");
scenario.run("alias=test2;type=diag;threads=1;interval=10;");
~~~

## Custom Scenarios

Of course, you can always write your own scenario script, using any valid javascript along with the built-in
engineblock scripting extensions. This is often done by a *scenario designer*.
 
Once you have a script, no matter how you came by it, you can simply run it like this:
~~~
./run-eb somescript
~~~

If your script takes named parameters, you can pass them in &lt;param&gt;=&lt;value&gt; ... form, such as
~~~
./run-eb somescript arg1=val1 arg2=val2
~~~


There are many other command line options available. For more advanced usage, 
consult the [Command Line Reference](command_line.md), or even the full
[Usage Guide](usage_guide.md)
