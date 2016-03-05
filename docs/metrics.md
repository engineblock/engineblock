## TestClient Metrics

If you like to have all of your testing data in one place, then you may be interested in reporting your measurements to a monitoring system. For this, test client includes a [Metrics Library](https://github.com/dropwizard/metrics). Graphite reporting is baked in as the default reporter.

In order to enable graphite reporting, use

    --graphite <host>
    
or

    --graphite <host>:<port>
    

## Metric Naming

### Prefix
Core metrics use the prefix _testclient_ by default. You can override this with the --prefix option:

    --prefix myclient.group5

### Identifiers

* Metrics associated with a specific activity will have the activity alias in their name.
* Metrics associated with a particular client driver, like the DataStax Java Driver for Cassandra, for example, should have "driver" in the name.

It is often helpful when instrumenting systems and apps to have a naming convention that works across all data sources. For this, I like to use the scheme _system | app_ . _client | server_ . ...

## Metrics Output

By default, the metrics will be logged to console via the console log and logging metrics reporter. At the end of a run, the long form of the metrics summaries are dumped to console. The reporting interval for this method is every minute. Once you start the client, you'll see a periodic report to the screen showing the current testing time as a heartbeat that the test is running.


## Interpretation & Examples

Some of these metric names may be different than what you will see in different activities. Consider them primarily as useful examples.

    22:27:20.544 [metrics-logger-reporter-thread-1] INFO  c.m.load.core.MetricReporters - type=COUNTER, name=ReadTelemetryAsyncActivity.async-pending, count=100

This line shows the basic format of a log line. The important bits here start with __type=COUNTER__. The remainder of this section will consist of only that part and everything after it. As well, the lines will be wrapped and indented to provide easier reading, and numbers shortened.

There will be a basic explanation of each type, followed by an explanation about the specific metric names.

__counters__

    type=COUNTER, name=ReadTelemetryAsyncActivity.async-pending, count=100

This is a basic counter. It simply tells you the number that the app was reporting at the time the reporter triggered. Counters are not montonically increasing. They can go up and down. In this example, the count shows you how many _async-pending_s there were, or the number of async operations in flight for the ReadTelemetryAsync activity.

__histograms__

	type=HISTOGRAM, name=ReadTelemetryAsyncActivity.tries-histogram,
     count=184148, min=1, max=1, mean=1.0, stddev=0.0, median=1.0,
     p75=1.0, p95=1.0, p98=1.0, p99=1.0, p999=1.0

	type=HISTOGRAM, name=ReadTelemetryAsyncActivity.ops-histogram,
     count=184148, min=2327168, max=176278812,
     mean=3.42E7,stddev=2.2264E7, median=2.938E7,
     p75=4.41E7, p95=7.8723E7, p98=9.61628E7, p99=1.12606E8, p999=1.703E8

This is a histogram of all the values submitted to it. It also contains basic stats of the values submitted, but no timing data apart from the value semantics of the samples themselves. In this case, the values are indicating the distribution of tries to complete the ReadTelemetryAsyncActivity. p999 is 1.0, so there is less than a 1/10000 chance that there was even a single retry.


__meters__

    type=METER, name=WriteTelemetryAsyncActivity.exceptions.PlaceHolderException,
    count=0, mean_rate=0.0, m1=0.0, m5=0.0, m15=0.0,
    rate_unit=events/second

A meter is merely a way to capture the rate of a type of an event. This one is the infamous PlaceHolderException, which is how I make sure that I'm reporting exactly 0 of something important to ensure that the downstream monitoring system can see it (and that the admin is putting something on the dashboard.) It captures not only the count, but also the mean_rate since start (which can skew away from recent trends) and the 1, 5 and 15 minute moving average.

__timers__

    type=TIMER, name=ReadTelemetryAsyncActivity.ops-total,
     count=184190, min=2.7, max=143.56, mean=34.48, stddev=22.24, median=29.52,
     p75=44.68, p95=80.63, p98=97.15, p99=108.48, p999=143.46,
     mean_rate=3081.74, m1=3489.71, m5=4171.31, m15=4312.,
     rate_unit=events/second, duration_unit=milliseconds

Timers are a combo of histograms and meters and counters. They include all the information we tend to want when when profiling something. In this case, millisecond measurements of the total time an operation took. Also, the moving average rates are included.The p999 duration is 143ms, not bad for my desktop system, and the median value (aka p50) is 34 microseconds. The average op rate for the duration of this test was 3081.

The remainder of this section describes the chosen metrics in more detail.

###### Cycle Position (counters)

- ReadTelemetryAsyncActivity.cycles
- WriteTelemetryAsyncActivity.cycles

The cycle position of the named activity. This is a trace of the progress between the start cycle and the end cycle, as specified on the command line.

###### Op rates & client latencies (timers)

- ReadTelemetryAsyncActivity.ops-total
- WriteTelemetryAsyncActivity.ops-total

The timing of an operation, from the time it was submitted asynchronously to the time it completed, including all retries, or the time it took to fail after exceeding retries.

###### Pending Async Ops (counters)

- WriteTelemetryAsyncActivity.async-pending
- WriteTelemetryAsyncActivity.async-pending

The number of asyncronous operations pending for the named activity.

###### Async Wait (timers)
- ReadTelemetryAsyncActivity.ops-wait
- WriteTelemetryAsyncActivity.ops-wait

The wait time between starting the getUninterruptibly() call and when it returns.

###### Tries, Retries (histograms)

- ReadTelemetryAsyncActivity.tries-histogram
- WriteTelemetryAsyncActivity.tries-histogram

###### Exception Rates (meters)

- WriteTelemetryAsyncActivity.exceptions.*
- ReadTelemetryAsyncActivity.exceptions.*

The rate of exceptions of the given name. There could be various names of this metric, so it's safer to monitor for everything under this name and then break them out individuall if you need.

###### Activity Count (counters)

- ActivityExecutorService.activities

The number of configured activities reported at the start of this test run.

## LICENSE

This is licensed under the Apache Public License 2.0
