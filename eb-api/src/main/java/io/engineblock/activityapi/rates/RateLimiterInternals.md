# Core Rate Limiter

This document elaborates on the design of the core rate limiters, to help
those who are curious or want to know how some of the design choices were made.

The rate limiter has to be fast enough that it doesn't hinder workloads. Further
it has to scale well under thread contention.

NOTE: This is not current with the split implementation. It does server, however,
to explain the core design of the strict rate limiter.

## Approaches 

The best source of basic high-resolution timing is System.nanoTime(), even though
accuracy varies. This provides a long value which works well with our technique,
described below. In terms of pausing a thread for a period of time, there are
multiple techniques that have been found with varying degrees of efficiency and
accuracy. We wish to avoid spin loops or other unconventional ways of forcing
timing precision in this case. Techniques which burn CPU cycles or avoid yielding
thread time back to the OS are wasteful of resources and thus steal valuable
capacity away from the testing instrument. For this reason, the two essential
ingredients to our approach are System.nanoTime() and Thread.sleep(millis,nanos).

## Unit of Work

By using nanos as a unit of work sizing, it is possible to track all grants for
time slots in terms of atomically incrementing long values, representing time
slices in nanoseconds. Simply put, a target rate of 500_000 yields a time slice
of 1_000_000_000 / 500_000, or 2000 nanoseconds. With this time slice, it is trivial
to atomically increment a moving accumulator that represents the time slices
given out, pacing each operation according to the accumulated value.

Further, the timer state is recorded in terms of views of actual time passing.

The ticks accumulator is one such counter. It simply tracks the monotonically
and atomically incrementing schedule pointer for the next available time slice.
When a caller calls the blocking acquire() method, the ticks accumulator already
holds the nanosecond time of the next available time slice. This value is
fetched and incremented atomically. The value left in the ticks accumulator is
the start time of the next time slice that will be given to a subsequent caller.

## Timing overhead

The bulk of time spent in a typical acquire() call is in the System.nanoTime()
call itself. This is problematic when you want to support high rate limits while
calling the system timer function every iteration. At a basic calling cost of
around 25ns each, the maximum theoretical limit that could be supported is
1000000000/25, or 40M ops/s. Single threaded, the Guava RateLimiter tops out at
around 25M ops/s on my 8 core system. However, we are not stuck with this as an
upper limit. It is important to optimize the timer not just to support higher
rates of operation, but also to preserve system thread time for the actual
workloads that need it.

At very high rate limits, timing accuracy becomes less critical, as other
elements of system flow will cause a degree of work spreading. Blocking queues,
thread pools, and SERDES will all make it difficult to preserve any sub-10ns
level timing downstream. For this reason, it is not critical to achieve sub-10ns
level isochronous dispatch at these rates. In short, fine-grained timing
precision is low priority. Rate discipline is high priority. These are not the
same thing. This offers room for an accuracy and speed trade-off.

The timer implementation thus keeps a view of system nanosecond passage of time
in another timeline register, called the *last-seen nanotime*. This means that
threads can share information about the last seen nanotime, avoiding calling it
except in cases that it may be needed in order to make progress on dispatch for
the current caller. The basic rule is that the last seen nanotime is only ever
updated to the current system nanotime IFF it has not advanced enough to unblock
the current caller. Doing so updates the shared view of the timeline register,
which again allows a number of subsequent callers to take the fast path for 
high op rates.

## Calling Overhead Compensation

The net effect of keeping a last-seen nanotime register is to allow for the
calling overhead of System.nanoTime() to be automatically mitigated, with any
saved nanoseconds being fed directly to the scheduling algorithm, which moves
scheduling logic into each thread for the optimal case. This creates an
automatic type of calling overhead compensation, wherein the more overhead
System.nanotime (and the acquire method itself) has in terms of time used, the
bigger the time gap created for rapid catch-up on calling threads. This does
create a possible type of leap-frogging effect in the timer logic. This should
be more pronounced as extremely high rates and less common at lower rates in
which threads are expected to be blocked more of the time. As well, it means
that in the catch-up case (the case in which last-seen doesn't need advancing in
order to make progress for the caller), the scheduling algorithm can do nearly
all stack-local and atomic accesses, which vastly speeds up the achievable op
rates.

## Strict vs Average

The two-timeline approach described above works well in terms of speed, but it
is not sufficient to control uniformity of schedule from one call to the next.

The reason for this is that as callers are delayed for their own reasons, the
"open timespan" gets larger. The longer time progresses without the ticks
accumulator following it, the larger the gap, and thus the availability of total
time at any one instant.

If you are targeting total average rate, this allows threads to effectively
burst over the rate limit in order to approach the average rate limit. Sometimes
this is desirable, but not as an unlimited bursting which might allow the short-term
measured op rate to be many times that of the target average rate.

However, you may prefer strict rate limiting from one call to the next. In other
words, you may require that with a 2000 ops/s rate limit, no two operations
occur less than 500_000 ns apart. This is called "Strict Rate Limiting".

## Burst Compensation

To support a mix of these two modes, a feature called *Burst Compensation* is
added to the scheduling algorithm. This feature takes any gap that exists
between the next available time slice and the current system nanotime (the 
open time span) and reduces it by a fraction. The fraction is a power of two, but
it is set loosely with a *ratio* of gap time, with an approximate register
shift occurring only in the case of a blocked caller. This has the effect of
soaking up lead time that is not being used, but in a proportional way.

A burst limit compensation value of 0.25 will cause 1/4 of the gap to be closed, 
a value of 0.125 will cause 1/8 to be closed, and so forth. Values in between 
are rounded down. This type of burst compensation is adaptive in that it will
allow longer periods of idle callers to provide more catch-up time while also
disallowing extended bursting to occur. Further study is needed to characterize
how this manifests for different caller timings.

As well, you may want to have the ability to catch up to an average target rate
with some amount of constant bursting. In other words, you may want to set a
target rate of R ops/s so long as the callers are keeping up with the allowed
rate, but allow them to go 110% R rate when behind to catch up. This is
represented with a strictness value of 1.1. Essentially, any strictness value
greater than 1.0 is considered a burst allowance. This works out nicely in
contrast to the burst compensation setting represented by strictness values less
than 1.0, since they are mutually exclusive.

A value of 0.0D disables limit compensation, which is will cause the rate
limiter to only throttle once the average rate is achieved. A value of 1.0D will
cause strict limiting between each iteration. A value of 1.15D will allow for
15% overspeed relative to the target rate. In practice none of these is best, as
thread pools have timing jitter, GCs occur, and systems need to warm up to their
achievable rates.

By default, the limit compensation is set to <s>1/32.</s> <em>TBD</em>

