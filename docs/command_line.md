# Engine Block Command Line

To get built-in help on the available command line options, run

~~~
java -jar eb.jar --help
~~~

The most common and useful commands will be explained at some depth in this guide, however
the built-in help above will be complete, with proper syntax for all commands.

There is more than one way to invoke EngineBlock. The sections below explain more about them, with more detail about how EngineBlock works as you read further.

__Running a pre-defined activity__

You may want to simply run one of the activities that are packaged with EB. This is the easiest way to use it, but also the one that offers the least amount of flexibility.

~~~
java -jar eb.jar activity alias=<alias> type=<type> param=<value> ...
~~~

This shows you how to run an activity and set the initial parameters for it.
have special characters in your activity parameters, you may need to use single or double quotes around the affected parameter, as in "parameter2=some value".

The activity option is, in fact, CLI sugar for scripting. It simply creates the script behind the scenes which will run the specified activities till they complete. In fact, this script snippet
is added at the end of the current script buffer, in the order that it appears in the command line.

__Running a scenario script__

If you actually do want to control the EB scenario with your own script logic, you can:

~~~
java -jar eb.jar script <scriptfile> param=value ...
~~~

This starts EngineBlock in script mode. In this mode, you provide a script which will control the scenario, starting activities, changing their behavior while they are running, and stopping them as you wish. This is called a Scenario Control Script, or simply `control script` for short.


With this mode, you can build test scenarios which are more sophisticated than a simple activity definition will allow. 

No matter how you start an EngineBlock scenario, a control script is always the outer-most level of logic.

The scripting runtime is simply Java 8's Nashorn. You can do anything in your script that it allows. Furthermore, elements of the [core runtime](core_concepts.md) are wired into the scripting sandbox. 

You can combine ``activity`` and ``script`` options in the same command line:

~~~
java -jar eb.jar \
activity alias=test1 type=diag threads=2 interval=50  \
script=script2.js \
activity alias=test3 type=diag threads=2 interval=50 
~~~

The order of execution is preserved. The only caveat is that activities that are added
in this way will be required to run completion before the scenario script exits. The point
at which they are inserted into the script buffer will determine when they are started. The
cycle parameter on each one will determine how many cycles it runs for before graceful exit.

__Learning about activities__

If you need to learn about what activity types are available in your runtime, their required parameters, etc, do this:

    java -jar eb.jar --list-activity-types
    
Each activity type that is known to your runtime will be listed.

You can also learn more about a particular activity type like this:
~~~
java -jar eb.jar help <activity type>
~~~

Most everything else that you need to know is covered elsewhere in the [Usage Guide](usage_guide.md). 

If you haven't already, it would be a good time to read about [core concepts](core_concepts.md).

