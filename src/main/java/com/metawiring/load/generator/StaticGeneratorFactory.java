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

package com.metawiring.load.generator;

import com.metawiring.load.generator.Generator;
import com.metawiring.load.generator.GeneratorFactory;
import com.metawiring.load.generators.ThreadNumGenerator;

public class StaticGeneratorFactory implements GeneratorFactory {
    private ThreadNumGenerator gen = new ThreadNumGenerator();
    public StaticGeneratorFactory(ThreadNumGenerator threadNumGenerator) {
        this.gen=threadNumGenerator;
    }

    @Override
    public Generator getGenerator() {
        return null;
    }
}
