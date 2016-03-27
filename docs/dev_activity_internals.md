Empirical Machine for Developers
=========================

## Activity Internals

Activities are a generalization of some type of client work that needs to occur to generate work against a test target. However, a purely abstract interface for activities would be so open-ended as to encourage no sense of commonality in testing semantics and controls. On the contrary, we do want some sense of isomorphism between activity types in terms of how they are implemented and reasoned about. After reading this document, you should now what it means to implement an activity properly.

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

### Thread Harness

Each ActivityExecutor uses the _Motor_ API to manage activity threads. A Motor is nothing new. The reason for the Motor abstraction to exists is to provide a more definite boundary between the machinery and the pluggable workloads. It provides a control boundary that is tangible to both the scripting runtime and the new concurrent programmer. For this reason, seasoned Java programmers will find nothing new or novel in the Motor abstraction.

### Slots, AKA Threads, AKA Motors

To support multiple signal routing topologies within an activity, the concept of a slot is used. A slot is nothing more than an indexed position for a thread in a thread pool. However, with the Motor being the control logic for each activity thread, it makes sense to call them "motor slots".

When a thread is being started for an activity, a motor instance is created for the slot, as well as an input and action instance. However, the ActivityType implementation has control of how these are created. If the ActivityType implementation chooses, it may return a unique input for each slot, or a single cached instance for all slots.

Essentially, each ActivityExecutor has this structure:

1. ActivityExecutor
   1. Indexed Thread Instance, each having
      1. a Motor instance, each having
         1. an Input instance (which may be shared, depending on ActivityType impl)
         2. an Action instance (which may be shared, depending on ActivityType impl)

### Activity Identification

The only way to address a running activity for dynamic control is through its _alias_. An alias is simply the name that the ScenarioController knows as the activity's name at runtime. If an alias is not provided, the runtime may accept a new activity, but it will be forced to generate an internal name for it.

## Iterating a Cycle

While an activity is running, each of its slots has a running motor which does the following continuously.

1. Verify Motor control state, stop if signalled
1. Read the next input value (a long) from the Input.
2. Apply the value to the Action.

The motor acts as a data pump, pulling in new test values to the application workload and turning the crank on the workload machinery. The consumer interface for an Action is very basic. This is intentional, and allows the maximum amount of flexibility in workload (AKA ActivityType) design. The motor control state is simply an atomically-visible breaker that is controlled by the ActivityExecutor.

The default implementation of an activity input is a sequence generator. This is what most activities will need. However, rate controls and other decorators may be desired, so the API makes it easy to wrap the default input.

### ActivityType Packaging & Discovery

_ActivityType_ implementations are discovered by the runtime using the 
[ServiceLoader API](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) , with the service name _com.metawiring.load.activities.ActivityType_. That means simply that you must add the fully-qualified class name of your ActivitType implementations to the META-INF/services/com.metawiring.load.activities.ActivityType file of your built jar.



