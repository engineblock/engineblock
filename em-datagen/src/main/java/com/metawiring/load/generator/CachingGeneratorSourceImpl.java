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

import com.metawiring.load.generators.*;

import java.util.HashMap;
import java.util.Map;

public class CachingGeneratorSourceImpl implements GeneratorInstanceSource {

    private GeneratorInstanceSource generatorInstantiator;
    private Map<String,Generator<?>> generators = new HashMap<>();

    public CachingGeneratorSourceImpl(GeneratorInstanceSource generatorInstantiator) {
        this.generatorInstantiator = generatorInstantiator;
    }

    public Generator getGenerator(String name) {
        Generator generator = generators.get(name);

        // extra code here avoids mandatory locking for reads
        if (generator==null) {
            synchronized(this) {
                generator = generators.get(name);
                if(generator==null) {
                    generator = generatorInstantiator.getGenerator(name);
                    generators.put(name,generator);
                }
            }
        }
        return generator;
    }

    @Override
    public String toString() {
        return "instantiator:" + generatorInstantiator +"\n" +
                " current generators:" + generators.keySet();
    }
}
