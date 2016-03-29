/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.microbench;

import com.codahale.metrics.Timer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.testng.annotations.Test;

@Test
public class TestDateTimeConversions {

    @Test(enabled = false)
    public void testTimerSpeed() {
        Timer t= new Timer();
        for (int n=0;n<10000000;n++) {
            Timer.Context ctx = t.time();
            ctx.stop();
        }
        System.out.println(t.getMeanRate());
        System.out.flush();
    }

    @Test(enabled = false)
    public void testJodaSpeed() {
        Timer t = new Timer();
        for (int n=0;n<10000000;n++) {
            Timer.Context ctx = t.time();
            DateTime dt = new DateTime(n);
            ctx.stop();
        }
        System.out.println(t.getMeanRate());
        System.out.flush();
    }

    @Test(enabled = false)
    public void testConvSpeed() {
        Timer t = new Timer();
        for (int n=0;n<10000000;n++) {
            Timer.Context ctx = t.time();
            DateTime dt = new DateTime(n);
            int year = dt.year().get();
            year +=0;
            ctx.stop();
        }
        System.out.println(t.getMeanRate());
        System.out.flush();
    }

    @Test(enabled = false)
    public void testCompositorSpeed() {
        Timer t = new Timer();
        StringBuilder sb = new StringBuilder(100);

        DateTimeFormatter format = DateTimeFormat.forPattern("YYYY-MM-dd-HH");

        for (int n=0;n<10000000;n++) {
            Timer.Context ctx = t.time();
            sb.setLength(0);
            DateTime dt = new DateTime(n);
            dt.toString(format);
            ctx.stop();
        }
        System.out.println(t.getMeanRate());
        System.out.flush();

    }
}
