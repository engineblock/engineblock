## EngineBlock Metrics

If you like to have all of your testing data in one place, then you may be interested in reporting your measurements to a monitoring system. For this, EB includes a [Metrics Library](https://github.com/dropwizard/metrics). Graphite reporting is baked in as the default reporter.

## Metrics Units of Measure

All metrics collected from activities are recorded in nanoseconds and ops per second.

In order to enable graphite reporting, use
~~~
    --report-graphite-to <host>
~~~    
or
~~~
    --report-graphite-to <host>:<port>
~~~

## Scripting with Metrics

There are enhancements to the metrics bundled with EngineBlock to support advanced scripting. See the [Advanced Metrics for Scripting](scripting.md#enhanced-metrics-for-scripting) in the scripting guide for more details.

## Metric Naming

### Prefix
Core metrics use the prefix _engineblock_ by default. You can override this with the --metrics-prefix option:

    --metrics-prefix myclient.group5

### Identifiers

Metrics associated with a specific activity will have the activity alias in their name. The names and types of metrics provided for each activity type vary. You can easily see what metrics are available for a given activity type:

~~~
java -jar eb.jar --show-metrics alias=test type=diag
~~~

This initializes an activity as it to run it, but never starts cycling it. 

## LICENSE

This is licensed under the Apache Public License 2.0
