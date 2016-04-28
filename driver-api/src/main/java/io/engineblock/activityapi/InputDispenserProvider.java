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

package io.engineblock.activityapi;

/**
 * InputDispenserProvider is the top level interface for constructing inputs.
 * However, InputDispenserProvider are ActivityType-scoped, meaning there is exactly one
 * InputDispenserProvider per ActivityType instance (aka engineblock driver) in the runtime.
 * If you mean to provide your own instancing or factory scheme for inputs, implement this
 * interface on your ActivityType and then implement the
 * {@link io.engineblock.activityapi.InputDispenserProvider#getInputDispensor(io.engineblock.activityapi.ActivityDef)}
 * method.
 */
public interface InputDispenserProvider {

    /**
     * Return the InputDispenser instance that will be used by the associated activity to create Input factories
     * for each thread slot.
     * @param activityDef an ActivityDef which will parameterize this InputDispenser
     * @return the InputDispenser for the associated activity
     */
    InputDispenser getInputDispensor(ActivityDef activityDef);
}
