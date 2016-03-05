TestClient for Developers
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

Some parameters are used to control the core threading behavior for an activity, as implemented in _ActivityExecutor_ and _ActivityMotor_ types. Some parameters are opaque to these classes, and are used strictly by the specific implementation of the activity itself. Each activity implementation has access to its parameter map, so additional parameters can be tacked on to an Activity which are implementation-specific.

For example, the ___threads___ parameter controls how many threads will concurrently execute a given activity. This parameter is configured with a value as an activity is declared, but it can be also modified on the fly. This causes the _ActivityExecutor_ to get notified, causing it to dynamically adjust the number of active threads. This is done via cooperative signaling and atomic values in a non-blocking way.

### Activity Identification
Every activity that is defined at runtime has be loaded from a vaid ___source___ address, be of a valid activity ___type___, and be named with a valid activity ___alias___.
 
 
In more detail:

1. The ___source___ can be any valid file path. It is simply the file that contains the activity details such as statements, data generators, etc.
2. The ___type___ determines which Activity implementation will be used to create the activity worker threads. It is a simple name which is provided by an Activity implemenation. __'cql'__ is a good example. The type will be inferred by looking at the source if it is not provided explicitly with a ___type=cql;___ parameter, for example. this is done simply by scanning the source name for an occurance of a known activity type name on non-word boundaries. For example, 'source=activity1_cql.yaml' would cause the 'cql' type to be assumed, but 'activity1cql.yaml' would not. Because of this mechanism, type os not kept as a canonical activity field in the short form example above, although it can be passed explicitly as desired.
3. The ___alias___ is simply a name by which an activity instance will be known during this test session. This is the name by which any metrics will be reported for this activity instance. It is also the name that must be used in order to control the activity during the current session, such as changing the concurrency, etc.

### Activity API

The _ActivityType_ interface is the starting point for implmenting a new Activity type.

The _ActivityMotor_ represents the per-thread logic of an activity. It is responsible for taking inputs from an instance of _ActivityInput_ and applying an _ActivityAction_ to them. In the simplest terms, each thread in an activity is controlled by an activity motor. This inner harness is also responsible for basic TestClient instrumentation, logging, and control signalling with the associated _ActivityExecutor_. This is where the aspects end, however, and activity-specific details begin.

Each Activity implementation is responsible for deciding how ActivityMotor instances are constructed, albeit indirectly. The hooks that determine the activity-specific behavior are implemented in terms of the core Java 8 _LongSupplier_ and _LongConsumer_ types. For each iteration of an _ActivityMotor_'s inner loop, another value is taken from the input (the LongSupplier). The action (the LongConsumer) is then called with that value.
 
### ActivityType Packaging & Discovery

_ActivityType_ implementations are discovered by the runtime using the 
[ServiceLoader API](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) , with the service name _com.metawiring.load.activities.ActivityType_. That means simply that you must add the fully-qualified class name of your ActivitType implementations to the META-INF/services/com.metawiring.load.activities.ActivityType file of your built jar.

### Semantics: Async Operations

The ___async___ parameter is used to determine how many operations will be kept in flight. Since this is different but related to ___threads___, it is important to keep in mind how they interact.  For the CQL driver, the asyncs are apportioned to all the threads in an activity, and re-apportioned when the activity controls are updated at runtime. Setting ___async___ lower than ___threads___ is invalid. If you are building a new activity type, this would be the recommended approach. This has the benefit of allowing for total-count sizing within an activity, but also avoiding costly semaphore-like resource management patterns.

If this is considered too difficult for a given client API, alternate approaches may be used. Consider having fixed number of asyncs per threads. In any case, please document the relationship between asyncs and threads in your implementation clearly. 

The next section has more details on implementing async operations in a consistent and useful way.

### Semantics: Unit of Progress

Inside the _ActivityMotor_, the inner-most loop is the cycle iterator. Each successful iteration represents the completion of one operational cycle sucessful or not. Given that a thread can be expected to juggle multiple async operations, how does this reconcile with "one operation per iteration?". Simply put, the inner loop is responsible for priming the request queue up to the number of async operations first, and then unconditionally processing one response out of it.

The net effect of this is that the motor loop has the following structure:
~~~
1. Submit operations until the request queue is primed 
   (the number of async operations allowed for this thread are in flight.)
2. Store the futures in a local queue.
3. Get the response from the oldest submitted future.
~~~
### Semantics: Async Exception Handling

In the case of a failed operation, you may want to emulate the normative approach of retrying an operation a certain number of times. However, due to the unit-of-progress semantics above, this means that such retries must be handled inside the loop, effectively reverting in to an iterative retry mode that is synchronous with respect to the failed operation. This adds a more detailed step to the list above:
~~~
4. Iteratively retry the operation in a synchronous loop until it either 
   succeeds or fails too many times.
~~~

This is how the CQL client is implemented. Specifically, an operation is allowed to be tried up to 10 times. During response handling, each time the operation fails, it is resubmitted after a delay of (tries * 0.1 seconds), so after the fourth exception, the current thread will delay 0.4 seconds. The total time an operation may synchronously block a thread in this case is 0.1 + 0.2 + 0.3 + ... = 5 seconds total.

The behavior above also represents a particular form of active backpressure-sensitivity. This is not an explicit form of back-pressure as provided by some system APIs. The messaging channel in this case is instead the thrown exceptions from the client, including synthesized time-out exceptions.

### Semantics: Load Sensitivity and Measurement

The inner loop is metered in various ways. Metrics are collected and submitted regularly to the configured metrics system. These metrics can provide some very interesting measures of system capacity and performance, given clear understanding of the client logic as described above. 

#### Metrics: Async Pending

This metric indicates a running count of operations that have been submitted asynchronously, prior to the result being processed by the client logic.

#### Metrics: Async Wait

This measures the amount of time that clients block for a response once the __getUnteruptibly()__ is called.

#### Metrics: Async Tries

This measures the number of tries needed to complete an operation. Each time an operation is submitted to the cluster, the number of total tries is added to the histogram. This is one of the most interesting metrics in terms of measuring load sensitivity.

During normal operations in which no operation is needed to be tried more than once, all histogram values will remain _1_. When every _1/10000th_ operation needs to be retried, the 99.99 pctile value will increase. This is a minor level of retry activity in the grand scheme, but may be significant for some use cases.

As the workload increases beyond a comfortable steady-state loading level for a given cluster, the counts will start to go up in the lower percentiles. If you have a seriously overloaded or mis-tuned cluster you may expect to see higher tries counts at lower percentiles. For example, if the 75th pctile measurement is above one at all, than that means at least 1/4th of your operations are failing at least once. This is way beyond the loading level that you would want to run a cluster in production.

Suffice it to say


