# Core Concepts

EngineBlock (EB) is a machine pattern for loadtest design and execution. This guide is intended to illustrate the moving parts and how they work together. In simplest form, EB serves as control wiring for a set of inputs, actions on those inputs, and instrumentation for measuring the result of those actions.

## Scenario

A runtime instance of EB runs a Scenario. A scenario is the sandbox within which multiple activities run concurrently.

## Activity

A runtime assembly of inputs actions, and measurements is called an **Activity**.

The data flow between an input and an action is driven by a per-thread harness called a **Motor**. Motors iterate on the input, reading from it each cycle and passing it to the action. 

Parallel motors run side-by-side when an Activity is running multi-threaded. These motors are managed by an Activity Executor. Each motor is explicitly assigned to a numbered slot when it is started.

## Activity Types

Runnable activities come in various pre-assembled forms, known as Activity Types. The available activity types are determined by the libraries which are packaged or deployed along with your EB distribution.

New ActivityTypes are merely implementations of the ActivityType interface. They are packaged for usage by including the appropriate ServiceLocator data in your jar. At the very minimum, an ActivityType implementation must define an Action implementation. The default implementation of the Motor and Input interfaces are sufficient for most needs.

Within a running scenario, running activities are known only by their aliases. That is, you must provide an alias when starting an activity.

## Inputs

For a given activity instance, there is usually a single input shared across all motors. This input is a sequence of longs which is accessed atomically. This allows the input to be used for rate control within the activity. It also allows for all motors to draw from a single known set of cycle numbers. This is important for some activity types which use sliding-window or other sequence-dependent logic.

## Actions

Actions are simply consumers of input values. The work done by actions represents the essential workload of a given test. Nearly always, the action implementation is what makes an activity type interesting.

### Scenario Script

When a Scenario runs, it is executed by a control script. Even when simpler modes of using EngineBlock are used, the control script is still running the show. For very basic tests, such as running a single workload against a target system, this may simply be a command to start the activity. For more complex scenarios, multiple activities may be started or stopped in various orders, with adjustments on the fly to controls such as thread count, delay settings, workload mix, etc. The open-ended scripting capabilities allow you to call into Java, into other javascript libraries, etc. Consult the section on [scripting](scripting.md) for more details.

### Activity Definitions

When you define an activity to run within a scenario, you are essentially setting the initial parameter values which are needed to start that activity. Some activity types need no parameters. Other activity types may require multiple parameters in order to start.

On the command line, an activity definition looks like this:
~~~
alias=activity1 type=diag threads=10 interval=1000
~~~

It is simply a map of parameter names and values. This map -- the activity definition -- is watched by running activities for changes.

#### Activity Control Parameters

All of the activity parameters mentioned in this documentation are provided to the scripting runtime as control variables. That means that as you modify their values, the EngineBlock runtime will notify any part of the running activity that cares to know. This allows you to create a test which is as simple or as sophisticated as needed, with dynamic changes to running workloads.


#### Activity Measurements

Some core measurements are made by the EngineBlock runtime. These measurements are exposed to the control script as variables. However, they will be read-only to the control script. This makes it possible to dynamically adapt a test to the measured results as the test proceeds.

#### Feedback Testing Method

The metrics provided act as feedback into your scenario script. The control parameters act as the output, in terms of changing behavior. Using these together, you can design a testing scenario that knows how to refine to a specific goal as it runs.
