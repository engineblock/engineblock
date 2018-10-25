package io.engineblock.metrics;

/*
 *
 * @author Sebastián Estévez on 10/25/18.
 *
 */


import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.mitchtalmadge.asciidata.graph.ASCIIGraph;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChartReporter extends ScheduledReporter {

    Map<String, ArrayList<Double>> p99sOverTime = new HashMap<>();

    public ChartReporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit, TimeUnit durationUnit) {
        super(registry, name, filter, rateUnit, durationUnit);
    }

    protected ChartReporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit, TimeUnit durationUnit, ScheduledExecutorService executor) {
        super(registry, name, filter, rateUnit, durationUnit, executor);
    }

    protected ChartReporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit, TimeUnit durationUnit, ScheduledExecutorService executor, boolean shutdownExecutorOnStop) {
        super(registry, name, filter, rateUnit, durationUnit, executor, shutdownExecutorOnStop);
    }

    protected ChartReporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit, TimeUnit durationUnit, ScheduledExecutorService executor, boolean shutdownExecutorOnStop, Set<MetricAttribute> disabledMetricAttributes) {
        super(registry, name, filter, rateUnit, durationUnit, executor, shutdownExecutorOnStop, disabledMetricAttributes);
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        for (String timerKey : timers.keySet()) {
            Snapshot snapshot = timers.get(timerKey).getSnapshot();
            ArrayList<Double> p99s = p99sOverTime.get(timerKey);
            if (p99s == null){
                p99s = new ArrayList<>();
            }
            p99s.add(snapshot.get99thPercentile());
            p99sOverTime.put(timerKey,p99s);
        }
    }

    public void generateChart(){
        for (Map.Entry<String, ArrayList<Double>> p99KV : p99sOverTime.entrySet()) {
            System.out.println(String.format("Charting p99 Latencies (in microseconds) over time (one second intervals) for %s:",p99KV.getKey()));
            double[] p99s = p99KV.getValue().stream().mapToDouble(Double::doubleValue).toArray(); //via method reference
            System.out.println(ASCIIGraph
                    .fromSeries(p99s)
                    .withNumRows(8)
                    .plot());
        }
    }
}
