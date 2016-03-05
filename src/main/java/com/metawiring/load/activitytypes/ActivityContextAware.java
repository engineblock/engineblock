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

package com.metawiring.load.activitytypes;

import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.core.OldExecutionContext;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;

public interface ActivityContextAware<C> {

    /**
     * Initialize the shared context. This should be done only once, presumably by a single instance of the activity.
     * The result will be given to all activitytypes to use as a form of inter-thread sharing. Any barrier conditions
     * should be embedded in the context implementation. This method will be called by whatever harness
     * is responsible for running the activity, and the result will be injected into all created instances.
     * @return A single shared context object instance of generic type C, to be used by all instances of this logical
     * activity.
     */
    C createContextToShare(ActivityDef def, ScopedCachingGeneratorSource genSource, OldExecutionContext executionContext);

    /**
     * Each logical instance of the activity should load the context. This method will be called by whatever harness
     * is responsible for running the activity.
     * @param sharedContext
     */
    void loadSharedContext(C sharedContext);

    /**
     * An ugly hack to allow parameterized cast, because type-erasure has always been an ugly hack, and we don't
     * want the harness to have to encode type for this managed resource. Simply return C, or face casting errors.
     * @return
     */
    Class<?> getSharedContextClass();
}
