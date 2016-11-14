/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package io.engineblock.metrics;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class HistoStatsCSVWriter {
    private final static Logger logger = LoggerFactory.getLogger(HistoStatsCSVWriter.class);
    private final File csvfile;
    FileWriter writer;
    private final static long logFormatVersion=1L;
    private long baseTime;

    public HistoStatsCSVWriter(File csvFile) {
        this.csvfile = csvFile;
        initFile(csvFile);
    }

    private FileWriter initFile(File logfile) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(logfile);
            writer = new FileWriter(logfile);
            return writer;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void write(String filedata) {
        try {
            writer.write(filedata);
            writer.flush();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void setBaseTime(long baseTime) {
        this.baseTime = baseTime;
    }

    public void outputComment(String s) {
            write("# " + s + "\n");
    }

    public void outputLogFormatVersion() {
        write("# log-format-version: " + logFormatVersion + "\n");
    }

    public void outputStartTime(long startTime) {
        write("# start-time: " + startTime + "\n");
    }

    public void outputLegend() {
        write("# TAG,INTERVALSTART,INTERVALLENGTH,DURATION,min,p25,p50,p75,p90,p95,p98,p99,p999,p9999,max\n");
    }

    public void writeInterval(Histogram h) {
        StringBuilder csvLine = new StringBuilder(1024);
        csvLine.append("Tag=").append(h.getTag()).append(",");
        Double start = ((double) h.getStartTimeStamp()-baseTime) / 1000.0D;
        Double end = ((double) h.getEndTimeStamp()-baseTime) / 1000.0D;
        Double len = end-start;
        csvLine.append(start).append(",");
        csvLine.append(len).append(",");
        csvLine.append(h.getMinValue()).append(",");
        csvLine.append(h.getValueAtPercentile(0.25D)).append(",");
        csvLine.append(h.getValueAtPercentile(0.50D)).append(",");
        csvLine.append(h.getValueAtPercentile(0.75D)).append(",");
        csvLine.append(h.getValueAtPercentile(0.90D)).append(",");
        csvLine.append(h.getValueAtPercentile(0.95D)).append(",");
        csvLine.append(h.getValueAtPercentile(0.98D)).append(",");
        csvLine.append(h.getValueAtPercentile(0.99D)).append(",");
        csvLine.append(h.getValueAtPercentile(0.999D)).append(",");
        csvLine.append(h.getValueAtPercentile(0.9999D)).append(",");
        csvLine.append(h.getMaxValue()).append("\n");
        write(csvLine.toString());

    }
}
