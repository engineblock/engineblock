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

package io.engineblock.activityapi.rates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Only have one thread calling System.nanoTime on a periodic basis.
 * TODO: Apply an initial microbench to calling overhead and target isochronous scheduling
 * to make it easier for users to make sense of timing behaviors.
 */
final class ClockTicker implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClockTicker.class);

    private final int interval;
    private final AtomicLong clockview = new AtomicLong(System.nanoTime());
    private boolean running = true;
    private long ticks=0L;
    private Thread thread;
    private static ClockTicker instance;


    public static ClockTicker get(int interval) {
        synchronized (ClockTicker.class) {
            if (instance==null) {
                instance = new ClockTicker(interval);
            } else if (instance.getInterval()!=interval) {
                throw new RuntimeException("Ensure that all delegated clock calls use the same interval.");
            }
        }
        return instance;
    }

    public int getInterval() {
        return interval;
    }

    private ClockTicker(int interval) {
        this.interval = interval;
        if (interval<1000) {
            logger.warn("clock ticker idle loop set to minimum of 1000, instead of " + interval);
            interval=1000;
        }
        thread = new Thread(this, "ClockTicker");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        this.running = false;
    }

    public long getTicks() {
        return ticks;
    }

    public AtomicLong getClockView() {
        return clockview;
    }

    @Override
    public void run() {
        if (interval > 500) {
            while (running) {
                long now = System.nanoTime();
                ticks++;
                clockview.set(now);
                try {
                    Thread.sleep(0, interval-300);
                } catch (InterruptedException ignored) {
                }
            }
        } else {
            logger.warn("This update interval is smaller than minimum sleep precision, it will be an expensive idle loop.");
            while (running) {
                long now = System.nanoTime();
                clockview.set(now);
            }
        }
    }

}
