## EngineBlock Metrics

### Metrics Units of Measure

All metrics collected from activities are recorded in nanoseconds and ops per second. All histograms are recorded with 4 digits of precision using HDR histograms.

### Reporting metrics to a collector
If you like to have all of your testing data in one place, then you may be interested in reporting your measurements to a monitoring system. For this, EB includes a [Metrics Library](https://github.com/dropwizard/metrics). Graphite reporting is baked in as the default reporter.

In order to enable graphite reporting, use one of these options formats:
~~~
    --report-graphite-to <host>
    --report-graphite-to <host>:<port>
~~~

### Scripting with Metrics

There are enhancements to the metrics bundled with EngineBlock to support advanced scripting. See the [Advanced Metrics for Scripting](scripting.md#enhanced-metrics-for-scripting) in the scripting guide for more details.

### Metric Naming

#### Prefix
Core metrics use the prefix _engineblock_ by default. You can override this with the --metrics-prefix option:

    --metrics-prefix myclient.group5

#### Identifiers

Metrics associated with a specific activity will have the activity alias in their name. The names and types of metrics provided for each activity type vary. You can easily see what metrics are available for a given activity type:

~~~
java -jar eb.jar --show-metrics alias=test type=diag
~~~

### 
This initializes an activity as if to run it, but never starts cycling it. 

### Recording HDR Histogram Logs
You can record details of histograms from any compatible metric (histograms and timers) with an option like this:
~~~
--log-histograms hdrdata.log
~~~
If you want to record only certain metrics in this way, then use this form:
~~~
--log-histograms '.*suffix:hdrdata.log'
~~~
Notice that the option is enclosed in single quotes. This is because the first part of the option value is a regex. The '.*suffix' pattern matches any metric name that ends with "suffix". Effectively, leaving out the pattern is the same as using '.\*', which matches all metrics.

As well, each metric can only be included in a single log. If multiple logs are specified that match a set of overlapping metric names, then only the first match will be honored. This makes the configuration order-specific. For example,
~~~
--log-histograms '.*ABC:hdr1.log' --log-histograms '.*BC:hdr2.log'
~~~
will cause anything that matches '.*ABC' to be logged in hdr1.log only, even though it would normally be matched by the second option. The reason for this requirement is that marking histogram intervals at arbitrary intervals can become expensive, so each logger config actually has a shadow copy of a histogram that it uses for its own scheduling. This method allows us to have a combination of accuracy as well as flexibility for logging, scripting, and downstream reporting.

If you want to specify the recording interval, use this form:
~~~
--log-histograms '.*suffix:hdrdata.log:5s'
~~~
If you want to specify the interval, you must use the third form.


## LICENSE

This is licensed under the Apache Public License 2.0
