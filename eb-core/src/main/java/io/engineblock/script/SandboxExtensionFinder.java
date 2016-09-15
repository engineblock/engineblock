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

package io.engineblock.script;

import io.engineblock.extensions.SandboxExtensionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public class SandboxExtensionFinder {

    private final static List<SandboxExtensionDescriptor<?>> extensionDescriptors = new ArrayList<>();

    public static List<SandboxExtensionDescriptor<?>> findAll() {
        if (extensionDescriptors.isEmpty()) {
            synchronized (SandboxExtensionFinder.class) {
                if (extensionDescriptors.isEmpty()) {
                    ServiceLoader<SandboxExtensionDescriptor> loader =
                            ServiceLoader.load(SandboxExtensionDescriptor.class);
                    loader.iterator().forEachRemaining(extensionDescriptors::add);
                }
            }
        }

        return Collections.unmodifiableList(extensionDescriptors);

    }
}
