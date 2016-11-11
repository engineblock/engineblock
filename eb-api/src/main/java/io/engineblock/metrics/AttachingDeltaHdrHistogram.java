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

/**
 * <p>Allow a DeltaHistogram to have an attached metric that also gets a copy of
 * any input data.</p>
 *
 * <p>Terms</p>
 * <ul>
 *     <li>Attaching metric - the original metric which maintains the attached metric.</li>
 *     <li>Attached metric - the "shadow" metric which receives the same input as the
 *     original metric.</li>
 * </ul>
 *
 * <p>Implementing classes should reproduce their configuration, but not their content.
 * This means that attached metrics will receive content from the time they are attached,
 * and wil not see previous content. Attaching metrics are responsible for the creation
 * and ownership of the attached metrics.</p>
 *
 * <p>Only one attached metric should be allowed. This does allow for
 * additional chaining, as long as only one metric is attached to each chaining
 * metric.</p>

 * <p>Implementations of the attaching metric should ensure that attached metrics
 * all receive the same data.</p>

 * <p>Implementations of attaching metric should not error when more than one
 * attachment is requested, however, a warning should be logged.</p>
 *
 * <p>Implementations should ensure that any naming fields are marked differently so that
 * the attached metric does not have the exact same name as the attaching metric.</p>
 */
public interface AttachingDeltaHdrHistogram extends DeltaHdrHistogram {
    /**
     * Attach a metric.
     * @return the attached metric, a new instance if none was attached before, and the extant instance if one was.
     */
    public DeltaHdrHistogram attach();
}
