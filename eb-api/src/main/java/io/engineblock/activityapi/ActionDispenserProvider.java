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

/**
 * ActionDispenserProvider is the top level interface for constructing actions.
 * However, ActionDispenserProvider are ActivityType-scoped, meaning there is exactly one
 * ActionDispenserProvider per ActivityType instance (aka engineblock driver) in the runtime.
 * If you mean to provide your own instancing or factory scheme for actions, implement this
 * interface on your ActivityType and then implement the
 * {@link io.engineblock.activityapi.ActionDispenserProvider#getActionDispenser(io.engineblock.activityapi.ActivityDef)}
 * method.
 */
public interface ActionDispenserProvider {
    /**
     * This method will be called <em>once</em> per action instance.
     *
     * @param activityDef The activity definition instance that will parameterize the returned ActionDispenser instance.
     * @return an instance of ActionDispenser
     */
    ActionDispenser getActionDispenser(ActivityDef activityDef);
}
