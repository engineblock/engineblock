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

package com.metawiring.tools.activityapi;

/**
 * An Activity represents a single type of operation. It may be iterated many times, perhaps even separately
 * in different threads. (non-shared)
 */
public interface Activity {


    /**
     * Initialize the Activity with the name and context.
     * @param name
     * @param context
     */
//    void init(String name, ExecutionContext context, ScopedCachingGeneratorSource scopedGeneratorSource);

    /**
     * Prepare the activity to do work.
     * The cycles are an (open,closed] form representation of the cycles which
     * this activity will be expected to run. Activities don't have to care about
     * these unless they need to know ahead of time, as in async ops where the {@link #iterate()}
     * method represents completions of operations.
     * @param startingCycle - the cycle number to start at
     * @param endingCycle - the cycle number to end at
     * @param maxAsync - total number of async operations this activity can have pending
     */
    void prepare(long startingCycle, long endingCycle, long maxAsync);

    /**
     * Called endingCycle - startingCycle  times after the {@link #prepare(long,long,long)} method
     */
    void iterate();

    /**
     * Called once after all calls to {@link #iterate()}
     */
    void cleanup();
}
