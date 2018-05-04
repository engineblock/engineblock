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

package io.engineblock.activityapi.core;

/**
 * If an action may have some cleanup or IO flushing to do after
 * the normal cycles are dispatched to it, then it can ask to be notified
 * one time by the motor after the end of input is seen.
 */
public interface BufferedAction extends Action {

    /**
     * This method is called on FlushableAction after all input for the
     * given thread is exhausted. This does not occur if the stopping
     * is caused by anything besides exhaustion of cycle input.
     */
    void flush();
}
