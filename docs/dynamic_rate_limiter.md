## EngineBlock Rate Limiter Design

### Requirements

Since a rate limiter in a test system is one point of unavoidable contention,
its design becomes a focus of critical concern, pun intended. As well, since
EngineBlock is designed to enable dynamic testing methods, general purpose
rate limiter designs are not sufficient. To be specific, a suitable rate
limiter for EngineBlock must include:

- wait time calculation
- strict rate limiting
- burst rate limiting
- high throughput
- reconfiguration

These elements will be described in more detail below for clarity. But first,
some elaboration of terms:

=TODO= Square up all terms

**requester** - In this document, _requester_ refers to the client logic
that uses the rate limiter. It can be thought of as a representative of
a client, a thread, or other process that needs to use a rate
limiter for the purposes of throttling request rates or packet rates.

**scheduled time** - The time at which an operation would start if all operations
before it were started on time, notwithstanding concurrency limits. 

**start time** - The time at which an operation starts. This can be determined by
external factors affecting the operations themselves or the rate limiter logic, or 
both.

**wait time** - The duration that passes between the scheduled time and the start
time. Conceptually, this is how long an operation waits in a queue until it starts
executing. When the wait time for an operation is zero, then the rate limiter is
known to be allowing operations to start at the target rate with no external
delays.

#### Requirement: Wait Time Calculation

This simply means that the rate limiter must be able to tell what the wait time is
for each operation. Since the rate limiter is the governor for when operations
are allowed to start, it is the only part of the system that can easily provide
wait time for use in other calculations.

#### Requirement: Strict Rate Limiting

When a minimum gap in time is ensured between any two operations, then the achieved
rate is strictly limited to how timely the request is submitted. In other words,
if a request is not made at the time the rate limiter would allow it to start,
the time is forfeited and all operations from that point must also be delayed by
that extra time.

#### Requirement: Burst Rate Limiting

In some cases, it is desirable to allow operations to execute temporarily at a
higher rate than normal, but only long enough to reduce the wait time back to
zero.

#### Requirement: Reconfiguration

Because EngineBlock is built to support dynamic workload adjustment, any rate
limiter it provides must also be dynamically adjustable. This simply means that
a rate limiter can be reconfigured on the fly without error, and without loss
of state.

#### Requirement: High Throughput

As mentioned above, a practical rate limiter imposes that contention is unavoidable between
multiple client threads. The requirement for high throughput merely emphasizes how
important it is for the rate limiter implementation to be as efficient as possible
for concurrent use. This means that some trade-offs in how it is implemented may
be necessary.


## Basic Design

The core rate limiter design uses a few distinct atomic values. Each of these
is conceptually an accumulator of nanoseconds which is modified atomically as
shared state across requester threads.

- n<sub>**A**</sub> - _allocated_nanos_ - The total time allocated to operations so far.
- n<sub>**I**</sub> - _idle_nanos_ - The total time given away as idle time.

Additionally, we may treat the system clock as such a value that we simply read.

- n<sub>**C**</sub> - _clock_nanos_ - The system clock.

1. When a rate limiter is initialized, the rate is converted to a number of nanoseconds per operation.
   This is called the _time_slice_ for the rate limiter.
2. When a caller requests to start, the limiter takes the current value in _allocated_nanos_ as the
   _scheduled_time_, and adds _time_slice_ to _allocated_ (for subsequent ops).
3. _op_delay_ is calculated as `clock_nanos - scheduled_time`. The meaning of op_delay is the
   duration between when the rate limiter would have allowed to the op to start to when
   the requester actually asked the rate limiter if it could start.
4. If _op_delay_ is negative, then the op is considered _early_ with respect to its scheduled time.
   The op can only be allowed to start in the future in order to make delay positive.
   This is another way of saying that the order of the clock time and op time is 1) clock 2) op.
   In this case, the limiter is required to add additional delay by holding the op in a sleep
   or other delay mechanism before it is allowed to proceed.
5. If _op_delay_ is positive, then the op is considered _late_ with respect to its scheduled time.
   It is in the past with respect to the clock time. This is another way of saying that the op and
   the clock are in order 1) op 2) clock. In this case, the op may immediately proceed.

## Design Trade-Offs

The above description shows a basic implementation of a rate limiter. However, due to calling overhead
incurred by simply reading the system clock, it is impossible to have accuracy at the level of
precision offered by nanosecond clock sources. Further, the accuracy of mechanisms for adding delay
is very poor. For example, it takes roughly 25ns to call `System.nanoTime()` and about 350ns to call
`Thread.sleep(millis,nanos)` once the VM is warmed up. This has some implications for trading off
wasted precision with valuable efficiency.

Given that the overhead of these calls makes nano-second timing accuracy unlikely even in the best case,
it is best then to relax the expectation of accuracy and simply avoid calling these functions for
every operation.

### Optimization: System.nanoTime()

The clock value is cached in a variable. If it is known by looking at this cached value that an
operation is already 'late', then calling System.nanoTime() again does nothing to improve the
accuracy beyond what is already achievable. Bypassing the call offers drastic speedups. Because
of this, System.nanoTime() is only called in the case that the op is possibly early, and then
the condition is rechecked to determine whether the op is actually early or late. The net effect
of this is that at higher rates, where the nanoTime() becomes a hotter source of contention,
it is actually called less with respect to the rate, but still maintains sufficient injection
of the system clock value to keep the accuracy errors bounded. 

Additionally, the number of times the fast path is executed before the system clock is updated
is bounded by a counter. It is much lighter cost to update a counter of fast path branches
than it is to call nanoTime(), so this yields a net speedup of around 50% on the top end of
rate limiter performance.

=TODO= Consider updating C without CAS

### Optimization: Thread.sleep(millis,nanos)

Worse than the calling overhead of System.nanoTime() is Thread.sleep(millis,nanos). While there are
other more arcane and timing capable methods of shaving off nanos where needed, these methods
are not efficient in a runtime that doesn't explicitly schedule all hardware threads. Thus,
to maintain overall efficiency, we resort to the standard primitives for slowing down threads
when needed: Sleeping. Still, calling an empty Thread.sleep(millis,nanos) has around 350ns overhead,
meaning that if you need to delay for less time than that with Sleep, you can't. In order to avoid the
inaccuracies of calling this method in this case, the delay threshold is further adjusted to
require more than 350 (actually 2x350) nanos of delay, or the op in question is simply allowed
to go unimpeded.

## Timing Accuracy

Because calling overhead, context switching, and other sources of jitter make it nearly impossible
to have precise and accurate timing at the nanosecond level, it should be made clear that
the rate limiter does not claim to do so. The rate limiter logic is tested in multiple ways,
including with synthetic clocks. Still, the testing is meant to ensure the baseline logic is
sane, and it can not account for system behavior in a real test. Results will vary, but
the design is focused on the need for maintaining a target rate at some *reasonable* level of
accuracy at some *usefully* high op rate.

## Maximum Throughput

# Other Things

- n<sub>**E**</sub> - _elapsed_ Nanos - The total amount of elapsed time according to the system clock.

## Sleep overhead compensation

An operation must be executed on or after its scheduled time, but not before.
This is the basic invariant that the rate limiter must ensure.

Given a rate per second _r_, and an operation in position
_p_ of a sequence of operations, the scheduled start time for that operation
is simply (r*p). However, the scheduled time is not predetermined for each and every
operation ahead of time. It is simply calculated by the rate limiter as requests
call it before starting an operation.



It is only the rate limiter that can determine when an operation may
actually be allowed to proceed, thus only the rate limiter can determine
the time difference between when an operation should have started and
when it actually started.
 