# Core Concepts
TestClient is a machine pattern for test design and execution. This guide is intended to illustrate the moving parts and how they work together.

## Elements
The core of testclient functions as control wiring for a set of inputs, actions on those inputs, and instrumentation to measure the result of those actions.

### Scenario

A runtime instance of testclient is called a Scenario. A scenario is the sandbox within which multiple activities run concurrently.

### Activity

An assembly of inputs, actions, and measurements is called an **Activity**. 

![Activity: Input-\>Action](Concepts_1.png "Basic Elements")


The data flow between an input and an action is driven by a per-thread harness called a **Motor**. Motors iterate on the input, reading from it each cycle and passing it to the action. 

Parallel motors run side-by-side when an Activity is running multi-threaded. These motors are managed by an Activity Executor. Each motor is explicitly assigned to a numbered slot when it is started.

## Activity Types

Activities come in various pre-assembled forms, known as Activity Types. These may be thought of as Drivers, or simply Activity implementations. The available activity types are determined by the libraries which are packaged or deployed along with your testclient distribution.

Activity Types are created by implementing the ActivityType interface. They are packaged for usage by including the appropriate ServiceLocator data in your jar. At the very minimum, an Activity Type implementation must define an Action implementation.

Within a running scenario, running activities are known only by their aliases. That is, you must provide an alias when starting an activity.

### Actions

Actions are simply consumers of input values. The work done by actions represents the essential workload of a given test. Often, the action implementation is what makes an activity type interesting.

### Inputs

For a given activity instance, there is usually a single input shared across all motors. This allows the input to be used for rate control within the activity. It also allows for all motors to draw from a single known set of cycle numbers. This is important for come activity types which use sliding-window or other sequence-dependent logic.

### Controls

Activities are defined and manipulated by a set of parameters called **Controls**. Controls can be changed while an activity is running to change its behavior during a test.

### Activity Definitions

When you define an activity to run within a scenario, you are essentially setting the initial control values which are needed by that activity. Some activity types need no parameters, only an alias, such as the "diag" activity. Other activity types may require multiple parameters in order to start.
The format is simple:

`alias=activity1;type=yamlcql;yaml=activities/read-random.yaml;threads=50;`

## Control Script

Activities may be controlled directly by JavaScript. Even when JavaScript isn't used directly by the user, it is still controlling the scenario. The script which is running the scenario is called the control script. For very basic tests, such as running a single workload against a target system, this may simply be a command to start the activity. For more complex scenarios, multiple activities may be started or stopped in various orders, with adjustments on the fly to controls such as thread count, delay settings, workload mix, etc.

## Active Variables

All of the controls mentioned in this documentation are provided to the scripting runtime as control variables. That means that as you modify their values, the testclient runtime is reacting to the changes. This allows you to create a test which is as simple or as sophisticated as needed. These variables are termed __Active Variables__, to call out the fact that they are directly bound to the behavior of the running scenario.

###  Measurements

Some core measurements are made by the testclient runtime. These measurements are exposed to the control script as active variables. However, they will be read-only to the control script. This makes it possible to dynamically adapt a test to the measured results as the test proceeds.

A full list of __Active Variables__ is available in the reference section. 

