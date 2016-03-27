# Empirical Machine Command Line

To get built-in help on the available command line options, run

    run-em -h

The most common and useful commands will be explained at some depth in this guide, however
the built-in help above will be complete, with proper syntax for all commands.

There is more than one way to invoke EM. The sections below explain more about them, explaining
more about the way EM works as you read further.

__Running a pre-defined activity__

You may want to simply run one of the activities that are packaged with EM. This is the easiest way to use it,
but also the one that offers the least amount of flexibility.

    java -jar em.jar --activity alias=<alias>;type=<type>;param=<value>;...
    
This shows you how to set activity parameters. An activity is configured to be started with the above format. If you ever
have special characters in your activity parameters, you may need to use single or double quotes around the activity
definition format, as appropriate.

The --activity option is, in fact, CLI sugar for a control script, but it does keep you from having to write said script.

__Running a scenario script__

If you actually do want to control the EM scenario with your own script logic, you can:

    java -jar em.jar --script <scriptfile>
    
This starts EM in script mode. In this mode, all activities must be managed from within the script.
The scripting runtime is simply Java 8's Nashorn. You can do anything in your script that it allows. Furthermore,
elements of the [core runtime](core_concepts.md) are wired into the scripting sandbox. With this, you can build
test scenarios which are more sophisticated than a simple activity definition will allow.

You can combine --activity and --script options in the same command line:

    java -jar em.jar \
    --activity alias=test1;type=diag;threads=2;interval=50; \
    --script=script2.js \
    --activity alias=test3;type=diag;threads=2;interval=50;
    
In this case, the order is not preserved. Activities listed will be prepended as script to start the listed activities.
    
__Learning about activities__

If you need to learn about available drivers (aka activity types), their required parameters, etc, do this:

    java -jar em.jar --list-activity-types

Each one should be listed with an example activity definition. (The key-value format above that follows --activity).

You can also learn more about a particular activity type like this:

    java -jar em.jar --activity-help <activity type>
    
Most everything else that you need to know is covered elsewhere in the [Usage Guide](usage_guide.md). If you haven't already,
it would be a good time to read about [core concepts](core_concepts.md).

