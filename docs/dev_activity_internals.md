Engine Block for Developers
=========================

## Activity Internals

Activities are a generalization of some type of client work that needs to occur to generate work against a test target. However, a purely abstract interface for activities would be so open-ended as to provide no useful common machinery at all. On the contrary, we do want some sense of isomorphism between activity types in terms of how they are implemented and reasoned about. After reading this document, you should now what it means to implement an activity properly-- building on the core machinery while adding in activity-type behavior appropriately. That is what an Activity Type is for -- filling in the difference between what the core machinery provides and what is needed to simulate a particular kind of application.

### ActivityTypes

Each activity that runs in EngineBlock is provided by an instance of an ActivityType. The first activity type that you will become familiar with is the ``diag`` activity type. An ActivityType is responsible for providing the application-like functionality that can be used in template form by activity instances. When you are ready, there is a section all about the basics of actually [implementing an activity type](dev_building_activities.md).

### Activity Parameters

All activities are controlled at runtime with a _ParameterMap_. This is simply an observable thread-safe map of configuration values in string-string form, with type-specific getters. It also supports a rudimentary form of parameter and argument encoding that looks like this example:
~~~
alias=activity1;source=activity1_cql.yaml;cycles=1..100;threads=10;async=100;
~~~
Sometimes, it is handy to have a shortened form of this, as long as you are familiar with the parameters. The canonical field names used above can be obviated as long as the values line up strictly. In other words, the following would be an equivalent specification for the above activity:
~~~
activity1;activity1_cql.yaml;1..100;10;100
~~~

### Thread Controls

At runtime, an activity is driven by a dedicated thread pool harness -- the ActivityExecutor. This harness knows how to adjust the running threads down or up, as needed by changes to the related _threads_ parameter. This is meaningful for a couple of reasons:

1. The client behavior can emulate typical threading dynamics of real applications more accurately than a task-and-queue-only abstraction.
2. The synthetic thread ID can be borrowed and used to directly map some measure of concurrency of data flow.
3. It is a familiar concurrency primitive that is used in many other testing tools.

### From ActivityTypes to Threads

There are a few lifetime scopes to keep in mind when a scenario is running. They are:
~~~
scenario
  control script
    activity
      thread
~~~
These scopes nest strictly from outside to inside. Activity-specific threads, labeled `motors` in the API, run within the activity. Their executors run in their own thread per-activity, and so forth.

The ActivityType interface, part of the core EngineBlock API, allows you to control how threads are created for activity instances, and how activity instances are created for an activity. This means that the API has two levels of instantiation and initialization, so some care has been taken to keep it as simple as possible, nonetheless. Here are the scoping layers above with some additional detail:

~~~
scenario has ActivityType instances
                   \/ which can provide
    activity has MotorDispensers
                   \/ which can dispense
      thread has Motors (Runnables)
~~~

The same basic scope applies to Motors, Inputs, and Actions. The naming scheme *...DispenserProvider* is used at the ActivityType API level, and the naming scheme *Dispenser* is used at the Activity instance level. This may seem like a naming shell game, but with the need to control creation of instances at two levels, there is no simply way to avoid it. It's better than making everything *...Factory* for sure.

To be clear, ActivityTypes can implement ActionDispenserProvider, which allows them to provide an instance of an ActionDispenser, which is scoped to the activity instance. Each activity instance will have exactly one dispenser for actions, motors and inputs. When a new Runnable is needed for an activity thread, these dispensers are called. The MotorDispenser provides the Runnable, and the input and action dispensers provide the rest, as needed by the Motor.

In practice, you don't have to think about the API at this level of detail. Most new ActivityType implementations will simply implement the API just enough to provide an Action implementation and nothing more. A basic annotated example is shown under [dev_annotated_diag.md](dev_annotated_diag.md).

### Why Motors?

Each ActivityExecutor uses the _Motor_ API to manage activity threads. A Motor is nothing new. The reason for the Motor abstraction to exists is to provide a more definite boundary between the machinery and the pluggable workloads. It provides a control boundary that is tangible to both the scripting runtime and the new concurrent programmer. For this reason, seasoned Java programmers will find nothing new or novel in the Motor abstraction. It's simply there to do the obvious things:

1. Enable (desired and actual) state signaling between executor and thread.
2. Represent the per-thread flow and execution of inputs and actions.
3. Instrument said inputs and actions for metrics.
4. Control the per-thread unit of work around longer-running, tighter iterations

Motors lifetimes are not per-cycle. Motors can hang around in an activity executor, be stopped, started, etc. They keep the same input and action assignments that they were assembled with

While it is possible to implement your own Motors, this will almost never be necessary.

### Slots, AKA Threads

To support multiple signal routing topologies within an activity, the concept of a slot is used. A slot is nothing more than an indexed position for a thread in a thread pool.

When a thread is being started for an activity, a motor instance is created for the slot, as well as an input and action instance. However, the ActivityType implementation has control of how these are created. If the ActivityType implementation chooses, it may return a unique input for each slot, or a single cached instance for all slots. This is controlled simply by the slot index, which is passed into the factory methods for motors, inputs and threads.

### Activity Alias

The only way to address a running activity for dynamic control is through its _alias_. An alias is simply the name that the ScenarioController knows as the activity's name at runtime. If an alias is not provided, the runtime may accept a new activity, but it will be forced to generate an internal name for it.

### ActivityType Name

ActivityTypes are discovered by the runtime via the Java ServiceLoader API. In addition to the basic Java type, an ActivityType instance has a name. For the built-in diagnostic activity type, it is 'diag'. Each activity type name must be unique at runtime, or an error is thrown.

With an activity alias and the activity type name, you have enough information to tell EngineBlock how to start an activity. The variable names for these are **alias** and **type**.

## Iterating a Cycle

While an activity is running, each of its slots has a running motor which does the following continuously.

1. Verify Motor control state, stop if signalled (a stop was requested)
2. Read the next input value (a long) from the Input, stop if exhausted
3. Apply the value to the Action.

The motor acts as a data pump, pulling in new test values to the application workload and turning the crank on the workload machinery. The consumer interface for an Action is very basic. This is intentional, and allows the maximum amount of flexibility in workload (AKA ActivityType) design. The motor control state is simply an atomically-visible breaker that is controlled by the ActivityExecutor.

The default implementation of an activity input is a sequence generator. This is what most activities will need. However, rate controls and other decorators may be desired, so the API makes it easy to wrap the default input.

### ActivityType Packaging & Discovery

_ActivityType_ implementations are discovered by the runtime using the 
[ServiceLoader API](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) , with the service name __io.engineblock.activityapi.ActivityType.__ That means simply that you must add the fully-qualified class name of your ActivityType implementations to the META-INF/services/io.engineblock.activityapi.ActivityType file of your built jar. A maven plugin automates this during build, and is explained in further detail in the dev guides.



