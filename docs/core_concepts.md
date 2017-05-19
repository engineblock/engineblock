# Core Concepts
EngineBlock (EB) is a machine pattern for loadtest design and execution. This guide is intended to illustrate the moving parts and how they work together.

## Scenario

EngineBlock executes performance testing scenarios in a Nashorn scripting environment. The scenario is in the form of a script, either constructed on-the-fly from the command line options, or directly created by a scenario designer. 

## Activity

A scenario can have multiple activities. Each activity runs independently of the others, and independently of the control flow of the scenario script. This means that all activities and the main scenario run concurrently. Activities typically have a set of dynamic parameters and realtime metrics which are exposed to the scripting environment. 

## Activity Type

Runnable activities come in various pre-assembled forms, known as Activity Types. The available activity types are determined by the libraries which are packaged or deployed along with your EngineBlock distribution.

## Activity Parameters

Each activity has a type, known simply as **type**. Each running activity must be also be given a nickname if you want to interact with it. This is called the activity **alias**. These are the two essential parameters that you provide to start any activity.

There will be other parameters in addition to these, according to the activity type. For example, the diag activity type understands the *interval* and *modulo* parameters, as these are essential configuration details for the *diag* activity type.

On the command line, an activity definition might look like this:
~~~
type=diag alias=activity1 threads=10 interval=1000 cycles=1000..2000
~~~

#### Standard Activity Parameters

These activity parameters are understood by all activities:

**type** - the name of the activity type, used to create the right kind of activity instance. This is a required parameter when starting any activity.

**alias** - the nickname of the activity while it is running. Used to provide a name under which you can interact with a running activity.

**threads** - the number of threads to run for an activity. *default*: 1

**cycles** - The range of input values to use. This can either be a simple count such as "500", or a range, like "500..1000". If you provide only a count, it is presumed to be a range from 0 to that number. All cycle ranges are "closed-open" intervals, meaning that a range of 0..500 actually uses 0 up to 499, leaving 500 unused. 

**targetrate** - The number of ops per second to limit the workload to.

**linkinput** - The name of another activity, whois input will govern the speed of an activity.

### Activity Modifiers

All of the activity parameters mentioned in this documentation are provided to the scripting runtime as variables. That means that as you modify their values, the EngineBlock runtime will notify any part of the running activity that cares to know. This allows you to create a test which is as simple or as sophisticated as needed, with dynamic changes to running workloads. For example, changing the value of threads for a running activity will actually adjust the number of running activity threads before the assignment returns.

### Activity Metrics

Some core measurements are made by the EngineBlock runtime. These measurements are exposed to the control script as variables. However, they will be read-only to the control script. This makes it possible to dynamically adapt a test to the measured results as the test proceeds.

### Feedback Testing Method

The metrics provided act as feedback into your scenario script. The scripting variables act as the controls. Using these together, you can design a testing scenario that knows how to refine to a specific goal as it runs. If you think of the activity as a workload on a test stand, the metrics on it as a dashboard, and the variables as controls, then you can create a script that acts as a testing automaton. Such scripts can implement everythign from an automated test battery to a dynamic analysis method.
