Command-Line Scripting
======================

Sometimes you want to to run a set of workloads in a particular order, or call other specific test setup logic in between phases or workloads. While the full scripting environment allows you to do this and more, it is not necessary to write javascript for every scenario.

For more basic setup and sequencing needs, you can achive a fair degree of flexibility on the command line. A few key API calls are supported directly on the command line. This guide explains each of them, what the do, and how to use them together.

## Script Construction

As the command line is parsed, from left to right, the scenario script is built in an internal scripting buffer. Once the command line is fully parsed, this script is executed. Each of the commands below is effectively a macro for a snippet of script. It is important to remember that order is important.

## Command line format

Newlines are not allowed when building scripts from the command line. As long as you follow the allowed forms below, you can simply string multiple commands together with spaces between. As usual, single word options without double dashes are commands, key=value style parameters apply to the previous command, and all other commands with --this-style are non-scripting options.

## Concurrency & Control

All activities that run during a scenario run under the control of, but independently from the scenario script. This means that you can have a number of activities running while the scenario script is doing its own thing. The scenario only completes when both the scenario script and the activities are finished.

## start type=&lt;activity type&gt; alias=&lt;alias&gt; ...

You can start an activity with this command. At the time this command is evaluated, the activity is started, and the script continues without blocking. This is an asynchronous start of an activity. If you start multiple activities in this way, they will run concurrently.

The type argument is required to identify the activity type to run. The alias parameter is not strictly required, unless you want to be able to interact with the started activity later. In any case, it is a good idea to name all your activities with a meaningful alias.

## stop &lt;alias&gt;

Stop an activity with the given alias. This is synchronous, and causes the scenario to pause until the activity is stopped. This means that all threads for the activity have completed and signalled that they're in a stopped state.

## await &lt;alias&gt;

Await the normal completion of an activity with the given alias. This causes the scenario script to pause while it waits for the named activity to finish. This does not tell the activity to stop. It simply puts the scenario script into a paused state until the named activity is complete.

## run type=&lt;activity type&gt; alias=&lt;alias&gt; ...

Run an activity to completion, waiting until it is complete before continuing with the scenario script.
It is effectively the same as **start type=&lt;activity type&gt; ... alias&lt;alias&gt; await &lt;alias&gt;**

## waitmillis &lt;milliseconds&gt;

Pause the scenario script for this many milliseconds. This is useful for controlling workload run duration, etc.

## script &lt;script file&gt;

Add the contents of the named file to the script buffer.

# An example CLI script

~~~
./run-eb \
start type=diag alias=activity_one cycles=10000000 \
start type=diag alias=activity_two cycles=20000000 \
waitmillis 10000 \
await activity_one \
stop activity_two
~~~

in this CLI script, the backslashes are necessary in order keep everything on the same command line. Here is a narrative of what happens when it is run.

1. An activity named 'activity_one' is started, with 1 million cycles of work.
2. An activity named 'activity_two' is started, with 2 million cycles of work.
3. While these activities run, the scenario script waits for ten seconds.
4. If activity_one is complete, the await returns immediately. If not, the script waits for activity_one to complete its 1000000 cycles.
5. activity_two is immediately stopped.
6. Because all activities are stopped or complete, and the script is complete, the scenario exits.


