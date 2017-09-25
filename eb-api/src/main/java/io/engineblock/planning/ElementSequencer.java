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

package io.engineblock.planning;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

public interface ElementSequencer<T> {

    int[] sequenceByIndex(List<T> elems, ToLongFunction<T> ratioFunc);

    default List<T> sequenceByElement(List<T> elems, ToLongFunction<T> ratioFunc) {
        int[] ints = sequenceByIndex(elems, ratioFunc);
        return Arrays.stream(ints).mapToObj(elems::get).collect(Collectors.toList());
    }

    default String sequenceSummary(List<T> elems, ToLongFunction<T> ratioFunc, String delim) {
        return sequenceByElement(elems,ratioFunc)
                .stream().map(String::valueOf).collect(Collectors.joining(delim));
    }
}
