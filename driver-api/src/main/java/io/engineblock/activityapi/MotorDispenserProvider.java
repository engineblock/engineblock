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
 * <p>MotorDispenserProvider is the top level interface for constructing motors. A motor is the
 * Runnable for threads within an activity.</p>
 *
 * <p>MotorDispenserProvider are ActivityType-scoped, meaning there is exactly one
 * MotorDispenserProvider per ActivityType instance (aka engineblock driver) in the runtime.
 * If you mean to provide your own instancing or factory scheme for actions, implement this
 * interface on your ActivityType and then implement the
 * {@link io.engineblock.activityapi.MotorDispenserProvider#getMotorDispenser(ActivityDef, InputDispenser, ActionDispenser)}
 * method.</p>
 *
 * <p>The MotorDispenser encapsulates both the input and the action dispensers. New threads in an
 * activity get a new motor from the activity's motor dispenser, which includes an input and an action,
 * according to the schemes provided by the InputDispenser and the ActionDispenser. This means that the
 * thread harness (Runnable) for a given slot may have complete thread-local objects, or there may
 * be some cross-slot sharing of thread-safe instances. Typically, the input will be shared by all slots
 * in an activity, while the motors and actions will be per-thread.</p>
 */

public interface MotorDispenserProvider {
    public MotorDispenser getMotorDispenser(ActivityDef activityDef, InputDispenser inputDispenser, ActionDispenser actionDispenser);
}
