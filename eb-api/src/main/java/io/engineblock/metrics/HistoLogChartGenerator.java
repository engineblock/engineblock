package io.engineblock.metrics;

/*
 *
 * @author Sebastián Estévez on 10/25/18.
 *
 */


import com.mitchtalmadge.asciidata.graph.ASCIIGraph;
import org.HdrHistogram.HistogramLogReader;
import org.HdrHistogram.Histogram;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class HistoLogChartGenerator {

    private static Map<String, ArrayList<Double>> p99sOverTime = new HashMap<>();

    public static void generateChartFromHistoLog(HistoIntervalLogger histoIntervalLogger) {
        File logFile = histoIntervalLogger.getLogfile();

        try {
            HistogramLogReader reader = new HistogramLogReader(logFile);

            while (reader.hasNext()){
                Histogram histogram = (Histogram)reader.nextIntervalHistogram();
                String tag = histogram.getTag();

                double value= (double)histogram.getValueAtPercentile(99)/1000;
                ArrayList<Double> valueList = p99sOverTime.get(tag);
                if (valueList == null){
                    valueList = new ArrayList<>();
                }
                valueList.add(value);
                p99sOverTime.put(tag, valueList);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

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
