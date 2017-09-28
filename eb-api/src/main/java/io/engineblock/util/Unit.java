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

package io.engineblock.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Unit {

    private final static Logger logger = LoggerFactory.getLogger(Unit.class);

    private static Pattern numberFmtPattern = Pattern.compile(" *(?<number>[0-9]+(\\.[0-9]+)?) *(?<unit>[^ ]+?)? *");
    private static long nanoPerSecond = 1000000000;
    private static long bytesPerGB = 1000000000;
    private static long BytesPerGiB = 1024 * 1024 * 1024;

    public static Optional<Long> msFor(String duration) {
        return durationFor(Duration.MS, duration);
    }

    public static Optional<Long> microsecondsFor(String duration) {
        return durationFor(Duration.US, duration);
    }

    public static Optional<Long> nanosecondsFor(String duration) {
        return durationFor(Duration.NS, duration);
    }

    public static Optional<Long> secondsFor(String duration) {
        return durationFor(Duration.SECOND, duration);
    }

    public static Optional<Long> minutesFor(String duration) {
        return durationFor(Duration.MINUTE, duration);
    }

    public static Optional<Long> durationFor(Duration resultUnit, String spec) {
        Matcher m = numberFmtPattern.matcher(spec);
        if (m.matches()) {
            String numberpart = m.group("number");
            Double base = Double.valueOf(numberpart);
            String unitpart = m.group("unit");
            if (unitpart != null) {
                Duration durationDuration = Duration.valueOfSuffix(unitpart);
                if (durationDuration == null) {
                    throw new RuntimeException("Unable to recognized duration unit:" + unitpart);
                }
                long specnanos = durationDuration.getNanos();
                long resultnanos = resultUnit.getNanos();
                double multiplier = (double) specnanos / (double) resultnanos;
                base = base * multiplier;
            }
            return Optional.of(base.longValue());
        } else {
            logger.error("Parsing error for specifier: '" + spec + "'");
            return Optional.empty();

        }
    }

    public static Optional<Double> countFor(String spec) {
        return convertCounts(Count.UNIT, spec);
    }

    public static Optional<Double> convertCounts(Count resultUnit, String spec) {
        Matcher m = numberFmtPattern.matcher(spec);
        if (m.matches()) {
            String numberpart = m.group("number");
            double base = Double.valueOf(numberpart);
            String unitpart = m.group("unit");
            if (unitpart != null) {
                Count specifierUnit = Count.valueOfSuffix(unitpart);
                if (specifierUnit == null) {
                    throw new RuntimeException("Unable to recognized getChainSize unit:" + unitpart);
                }
                double specifierScale = specifierUnit.getMultiplier();
                double resultScale = resultUnit.getMultiplier();
                double multiplier = (specifierScale / resultScale);
                base *= multiplier;
            }
            return Optional.of(base);
        } else {
            logger.error("Parsing error for specifier:'" + spec + "'");
            return Optional.empty();
        }

    }

    public static Optional<Double> bytesFor(String spec) {
        return convertBytes(Bytes.BYTE, spec);
    }

    public static Optional<Double> convertBytes(Bytes resultUnit, String spec) {
        Matcher m = numberFmtPattern.matcher(spec);
        if (m.matches()) {
            String numberpart = m.group("number");
            double base = Double.valueOf(numberpart);
            String unitpart = m.group("unit");
            if (unitpart != null) {
                Bytes specifierUnit = Bytes.valueOfSuffix(unitpart);
                if (specifierUnit == null) {
                    throw new RuntimeException("Unable to recognized duration unit:" + unitpart);
                }
                long specifierScale = specifierUnit.getBytes();
                long resultScale = resultUnit.getBytes();
                double multiplier = (double) specifierScale / (double) resultScale;
                base *= multiplier;
            }
            return Optional.of(base);
        } else {
            logger.error("Parsing error for specifier:'" + spec + "'");
            return Optional.empty();
        }

    }

    public static enum Count {
        UNIT("U", "unit", 1.0),
        KILO("K", "kilo", 1000.0),
        MEGA("M", "mega", 1000000.0),
        GIGA("G", "giga", 1000000000.0),
        TERA("T", "tera", 1000000000000.0),
        PETA("P", "peta", 1000000000000000.0),
        EXA("E", "exa",   1000000000000000000.0);

        private final String label;
        private final String name;
        private final double multiplier;

        Count(String label, String name, double multiplier) {
            this.label = label;
            this.name = name;
            this.multiplier = multiplier;
        }

        public static Count valueOfSuffix(String suffix) {
            for (Count count : Count.values()) {
                if (count.toString().toLowerCase().equals(suffix.toLowerCase())) {
                    return count;
                }
                if (count.label.toLowerCase().equals(suffix.toLowerCase())) {
                    return count;
                }
                if (count.name.toLowerCase().equals(suffix.toLowerCase())) {
                    return count;
                }
            }
            return null;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }

    public static enum Bytes {
        BYTE("B", "byte", 1),
        KB("KB", "kilobyte", 1000),
        MB("MB", "megabyte", 1000000),
        GB("GB", "gigabyte", bytesPerGB),
        TB("TB", "terabyte", bytesPerGB * 1000),
        PB("PB", "petabyte", bytesPerGB * 1000000),
        EB("EB", "exabyte", bytesPerGB * bytesPerGB),
        ZB("ZB", "zettabyte", bytesPerGB * bytesPerGB * 1000),
        YB("YB", "yottabyte", bytesPerGB * bytesPerGB * 1000000),

        KIB("KiB", "kibibyte", 1024),
        MIB("MiB", "mebibyte", 1024 * 1024),
        GIB("GiB", "gibibyte", BytesPerGiB),
        TIB("TiB", "tebibyte", BytesPerGiB * 1024),
        PIB("PIB", "pebibyte", BytesPerGiB * 1024 * 1024),
        EIB("EiB", "exbibyte", BytesPerGiB * BytesPerGiB),
        ZIB("ZiB", "zebibyte", BytesPerGiB * BytesPerGiB * 1024),
        YIB("YiB", "yobibyte", BytesPerGiB * BytesPerGiB * 1024 * 1024);

        private final String name;
        private final long bytes;
        private final String label;

        Bytes(String label, String name, long bytes) {
            this.label = label;
            this.name = name;
            this.bytes = bytes;
        }

        public static Bytes valueOfSuffix(String unitpart) {
            for (Bytes byteUnit : Bytes.values()) {
                if (byteUnit.label.toLowerCase().equals(unitpart.toLowerCase())) {
                    return byteUnit;
                }
                if (byteUnit.name.toLowerCase().equals(unitpart.toLowerCase())) {
                    return byteUnit;
                }
                if ((byteUnit.name.toLowerCase() + "s").equals(unitpart.toLowerCase())) {
                    return byteUnit;
                }
                if (byteUnit.toString().toLowerCase().equals(unitpart.toLowerCase())) {
                    return byteUnit;
                }
            }
            return null;
        }

        public long getBytes() {
            return bytes;
        }
    }

    public static enum Duration {
        SECOND("s", "seconds", nanoPerSecond),
        MS("ms", "milliseconds", 1000000),
        US("µs", "microseconds", 1000),
        NS("ns", "nanoseconds", 1),
        MINUTE("M", "minutes", nanoPerSecond * 60),
        HOUR("H", "hours", nanoPerSecond * 60 * 60),
        DAY("D", "days", nanoPerSecond * 60 * 60 * 24),
        WEEK("W", "weeks", nanoPerSecond * 60 * 60 * 24 * 7),
        YEAR("Y", "years", nanoPerSecond * 60 * 60 * 24 * 365);


        private final String name;
        private final String label;
        private final long nanos;

        Duration(String label, String name, long nanos) {
            this.label = label;
            this.name = name;
            this.nanos = nanos;
        }

        public static Duration valueOfSuffix(String spec) {
            for (Duration duration : Duration.values()) {
                if (duration.label.toLowerCase().equals(spec.toLowerCase())) {
                    return duration;
                }
                if (duration.toString().toLowerCase().equals(spec.toLowerCase())) {
                    return duration;
                }
                if (duration.name.toLowerCase().equals(spec.toLowerCase())) {
                    return duration;
                }
            }
            return null;
        }

        public long getNanos() {
            return nanos;
        }
    }


}
