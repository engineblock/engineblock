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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Unit {

    private static Pattern numberFmtPattern = Pattern.compile(" *(?<number>[0-9]+(\\.[0-9]+)?) *(?<unit>[^ ]+?)? *");
    private static long nanoPerSecond = 1000000000;

    public static long msFor(String duration) {
        return durationFor(Duration.ms, duration);
    }
    public static long microsecondsFor(String duration) {
        return durationFor(Duration.us, duration);
    }
    public static long nanosecondsFor(String duration) {
        return durationFor(Duration.ns, duration);
    }
    public static long secondsFor(String duration) {
        return durationFor(Duration.SECOND, duration);
    }
    public static long minutesFor(String duration) {
        return durationFor(Duration.MINUTE, duration);
    }


    public static long durationFor(Duration resultUnit, String spec) {
        Matcher m = numberFmtPattern.matcher(spec);
        if (m.matches()) {
            String numberpart = m.group("number");
            Long base = Long.valueOf(numberpart);
            String unitpart = m.group("unit");
            if (unitpart != null) {
                Duration durationDuration = Duration.valueOfSuffix(unitpart);
                if (durationDuration == null) {
                    throw new RuntimeException("Unable to recognized duration unit:" + unitpart);
                }
                long specnanos= durationDuration.getNanos();
                long resultnanos = resultUnit.getNanos();
                double multiplier = (double) specnanos / (double) resultnanos;
                base = (long) ((double)base * multiplier);
            }
            return base;
        } else {
            throw new RuntimeException("Unable to match duration specifier:'" + spec + "'");
        }
    }


    public static double bytesFor(String spec) {
        return convertBytes(Bytes.BYTE,spec);
    }

    public static double convertBytes(Bytes resultUnit, String spec) {
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
                base*=multiplier;
            }
            return base;
        } else {
            throw new RuntimeException("Unable to match duration specifier:'" + spec + "'");
        }

    }

    private static long bytesPerGB=1000000000;
    private static long BytesPerGiB=1024*1024*1024;

    public static enum Bytes {
        BYTE("B","byte",1),
        KB("KB","kilobyte", 1000),
        MB("MB","megabyte", 1000000),
        GB("GB","gigabyte", bytesPerGB),
        TB("TB","terabyte", bytesPerGB*1000),
        PB("PB","petabyte", bytesPerGB*1000000),
        EB("EB","exabyte", bytesPerGB*bytesPerGB),
        ZB("ZB","zettabyte", bytesPerGB*bytesPerGB*1000),
        YB("YB","yottabyte", bytesPerGB*bytesPerGB*1000000),

        KIB("KiB","kibibyte",1024),
        MIB("MiB","mebibyte",1024*1024),
        GIB("GiB","gibibyte",BytesPerGiB),
        TIB("TiB","tebibyte",BytesPerGiB*1024),
        PIB("PIB","pebibyte",BytesPerGiB*1024*1024),
        EIB("EiB","exbibyte",BytesPerGiB*BytesPerGiB),
        ZIB("ZiB","zebibyte",BytesPerGiB*BytesPerGiB*1024),
        YIB("YiB","yobibyte",BytesPerGiB*BytesPerGiB*1024*1024);

        private final String name;
        private final long bytes;
        private String label;

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
                if ((byteUnit.name.toLowerCase()+"s").equals(unitpart.toLowerCase())) {
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
        SECOND("s", "SECOND", nanoPerSecond),
        ms("ms", "milliseconds", 1000000),
        us("µs", "microseconds", 1000),
        ns("ns", "nanoseconds", 1),
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
