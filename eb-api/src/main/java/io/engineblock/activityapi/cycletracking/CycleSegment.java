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

package io.engineblock.activityapi.cycletracking;

import java.util.Arrays;

/**
 * This is just a typed-data holder for efficient transfer of tracked data.
 * It holds a base cycle value, and a byte buffer view of cycle values.
 */
public class CycleSegment {

    /**
     * The base cycle value, the minimum cycle in the segment.
     */
    public long cycle;

    /**
     * A view of status codes in byte form.
     */
    public byte[] codes;

    public static CycleSegment forData(long cycle, byte[] buffer, int offset, int len) {
        CycleSegment s = new CycleSegment();
        s.cycle = cycle;
        s.codes = Arrays.copyOfRange(buffer,offset,offset+len);
        return s;
    }
}
