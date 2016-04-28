/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package io.engineblock.activityapi;

import java.util.function.LongConsumer;

/**
 * An action is the core logic that occurs within an activity.
 * Within a thread slot, a motor will continuously ask an action to process its input.
 */
public interface Action extends LongConsumer {

    /**
     * <p>Apply a work function to an input value.</p>
     *
     * @param value a long input
     */
    @Override
    void accept(long value);
}
