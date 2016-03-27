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

