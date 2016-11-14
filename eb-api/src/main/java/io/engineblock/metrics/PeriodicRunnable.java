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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a simple and light way to run a periodic task run
 */
class PeriodicRunnable<T extends Runnable> implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(PeriodicRunnable.class);

    private long intervalMillis;
    private T action;

    public PeriodicRunnable(long intervalMillis, T action) {
        this.action = action;
        this.intervalMillis = intervalMillis;
    }

    public void startThread() {
        Thread intervalThread = new Thread(this);
        intervalThread.setDaemon(true);
        intervalThread.setName(action.toString());
        intervalThread.start();
    }

    @Override
    public void run() {
        long nextEventTime = System.currentTimeMillis() + intervalMillis;
        while (true) {
            nextEventTime = awaitTime(intervalMillis, nextEventTime);
            logger.trace("invoking interval runnable " + action);
            System.out.println("running action: " + action);
            action.run();
        }
    }

    private long awaitTime(long interval, long nextEventTime) {
        long duration = nextEventTime - System.currentTimeMillis();
        while (System.currentTimeMillis() < nextEventTime) {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException ignored) {
            }
        }
        return nextEventTime + interval;
    }

}
