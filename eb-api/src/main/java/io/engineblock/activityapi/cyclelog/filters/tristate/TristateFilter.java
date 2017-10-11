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

package io.engineblock.activityapi.cyclelog.filters.tristate;

import java.util.function.Function;

/**
 * A tri-state filter allows for flexible configuration of
 * multi-phase filtering. It effectively allows a conditional behavior
 * for filtering logic that can answer "yes", "no", <em>and</em> "I don't know."
 *
 * This can also be used to build classic bi-state filtering, such as
 * filters that use a boolean predicate to say "keep" or "discard." Where the
 * tri-state filtering pattern shines, however, is in the ability to combine
 * different filtering rules to build a sophisticated filter at run-time
 * that bi-state filtering would prevent.
 *
 * In contrast to the bi-state filter, the default policy that is applied when
 * <em>not</em> matching an item with the predicate is to simply ignore it.
 * This means that in order to implement both matching and discarding
 * policies like a bi-state filter, you must do one of the following:
 * <ul>
 *     <li>Implement a default policy that overrides the "Ignore" action.</li>
 *     <li>Use both "keep" and "discard" predicates together in sequence.</li>
 * </ul>
 *
 * The two techniques above are not mutually exclusive. In practice a tri-state
 * filter will already include a default override for cases in which no
 * predicate matched an element. These are known as "inclusive" or "exclusive"
 * with respect to their default policy.
 */
public interface TristateFilter<T> extends Function<T, TristateFilter.Policy> {

    @Override
    Policy apply(T cycleResult);

    /**
     * If the predicate for this filter matches, then the action associated
     * with it will
     * The filter action determines what action is taken for a given
     * element that matches the predicate. If the whether to include or exclude a result
     * of the filter matching. If the filter does not match, then neither
     * include nor exclude are presumed. See the class docs for more details.
     */
    public enum Policy {
        Keep,
        Discard,
        Ignore
    }

}
