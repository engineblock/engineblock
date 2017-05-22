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

/**
 * <p>A cycle tracker allows concurrent threads to report on the
 * status of a cycle-oriented task, like an activity.</p>
 *
 * Trackers are types that know how read data that is collected
 * at runtime by a {@link CycleResultSink}.
 *
 * <p>When an activity is configured with a marker, it must mark
 * completion on a specific cycle when it is complete, independent
 * of the order of completion.</p>
 *
 * <p>It is the job of the tracker to provide efficient concurrent access
 * to:
 * <UL>
 *     <LI>The lowest cycle number that has been isCycleCompleted. This may return a lower
 *     cycle number that has been isCycleCompleted within some bound, but it may not be higher..</LI>
 *     <LI>The highest cycle number that has been isCycleCompleted. This may return a lower cycle
 *     number than the highest cycle number that has been isCycleCompleted, but it must never be higher.
 *     </LI>
 * </UL>
 */
public interface Tracker extends CycleResultSink, CycleResultSource {

}
