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
--log-histograms 'hdrdata.log:.*suffix'
~~~
Notice that the option is enclosed in single quotes. This is because the second part of the option value is a regex. The '.*suffix' pattern matches any metric name that ends with "suffix". Effectively, leaving out the pattern is the same as using '.\*', which matches all metrics.

Metrics may be included in multiple logs, but care should be taken not to overdo this. Keeping higher fidelity histogram reservoirs does come with a cost, so be sure to be specific in what you record as much as possible.

If you want to specify the recording interval, use this form:
~~~
--log-histograms 'hdrdata.log:.*suffix:5s'
~~~
If you want to specify the interval, you must use the third form, although it is valid to leave
the pattern empty, such as 'hdrdata.log::5s'.

### Recording HDR Histogram Stats
You can also record basic snapshots of histogram data on a periodic interval just like above with
HDR histogram logs. The option to do this is:
~~~
--log-histostats 'hdrstats.log:.*suffix:10s'
~~~
Everything works the same as for hdr histogram logging, except that the format is in
CSV as shown in the example below:
~~~
#logging stats for session scenario-1479089852022
#[Histogram log format version 1.0]
#[StartTime: 1479089852.046 (seconds since epoch), Sun Nov 13 20:17:32 CST 2016]
#Tag,Interval_Start,Interval_Length,count,min,p25,p50,p75,p90,p95,p98,p99,p999,p9999,max
Tag=diag1.delay,0.457,0.044,1,16,31,31,31,31,31,31,31,31,31,31
Tag=diag1.cycles,0.48,0.021,31,4096,8191,8191,8191,8191,8191,8191,8191,8191,8191,2097151
Tag=diag1.delay,0.501,0.499,1,1,1,1,1,1,1,1,1,1,1,1
Tag=diag1.cycles,0.501,0.499,498,1024,2047,2047,4095,4095,4095,4095,4095,4095,4095,4194303
...
~~~

Notice that the format used is similar to that of the HDR logging.

## LICENSE

This is licensed under the Apache Public License 2.0
