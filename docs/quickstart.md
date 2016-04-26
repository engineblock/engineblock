# Quick Start

## Installation

EB is released as an executable jar. To use a specific release, simply download it and run it like this:
    
    curl -O https://github.com/jshook/em/releases/download/1.0.0-snapshot/eb.jar
    java -jar eb.jar
        
The latest release can always be run with this simple script: [run-em](https://raw.githubusercontent.com/jshook/em/run-em). You can use the commands below to get it. It will simply download eb.jar from the latest release if needed. As always, blindly running scripts from the web is not a good idea. Look at this script before you run it.

    curl -O https://raw.githubusercontent.com/jshook/em/archwork/run-em
    chmod u+x run-em

This script may be used in place of

    java -jar eb.jar arg0 ...
    
It will have the same effect as running 'java -jar eb.jar' with the supplied command line arguments, except that it will fetch eb.jar if needed. The rest of the examples will use this method, although you can simply invoke the jar directly if you need.

# Usage

__Available Activity Types__

    ./run-em --list-activity-types

This should provide a list of the activity types that are known to the client. The build-in activity type "diag", is always available. To really be useful, you need to add activity type libraries to the libs directory.

__Activity Type Docs__

You can get further documentation about an activity...

    ./run-em --activity-help diag

This will show the internal documentation for the named activity type. There will be documentation for every activity, as the activity loader will not recognize one without it.

__Running an Activity__

You can run an instance of an activity:

    ./run-em --activity alias=test1;type=diag;threads=10;interval=200;

This shows the standard form of an activity definition. It is simply a map of activity parameters and values.
Here is what they do:

- __type__ - All activities must have a type. This determines which ActivityType implementation is used to run the activity.
- __alias__ - All dynamically controlled activities must have an alias. The activity alias is the only way to modify a running activity once it is started. Think of the alias as the handle to the activity instance.
- __threads__ - All activities have this parameter. If you do not specify it, it defaults to 1. This determines how many threads are run concurrently within the activity.

You can also run multiple activities:

    ./run-em \
    --activity alias=test1;type=diag;threads=10;interval=200; \
    --activity alias=test2;type=diag;threads=1;interval=10;

__Scripting__

    ./run-em some

## The Command Line

There are many other command line options available. For more advanced usage, 
consult the [Command Line Reference](command_line.md), or even the full
[Usage Guide](usage_guide.md)