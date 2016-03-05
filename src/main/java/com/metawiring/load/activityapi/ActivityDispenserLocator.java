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

package com.metawiring.load.activityapi;

import com.metawiring.load.config.ActivityDef;

import java.util.Optional;

/**
 * This is how we find activitytypes in the system.
 */
public interface ActivityDispenserLocator {
    /**
     * If possible, create an activity dispenser that knows how to create Activities,
     * honoring the scoping rules
     */
    Optional<? extends ActivityDispenser> resolve(ActivityDef activityDef);

}
