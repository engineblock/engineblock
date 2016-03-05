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

package com.metawiring.load.core;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSetFuture;

import java.util.concurrent.Future;

/**
 * This is meant to be used internally by activity implementations, not shared in any way.
 *
 * You can use it to track the status of an async operation so that it may be retried if needed.
 */
public class TimedFuture<T extends Future> {

    public T future;

    // initialize this before you send the first request for this bound statement
    public Timer.Context timerContext;

    // The bound statement, if the result is not successful, simply resend it
    public BoundStatement boundStatement;

    // Incremented for each executeAsync
    public int tries = 0;
}
