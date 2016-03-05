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

import com.metawiring.load.generators.CycleNumberGenerator;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class GeneratorInstantiator implements GeneratorInstanceSource {
    private final static Logger logger = LoggerFactory.getLogger(GeneratorInstantiator.class);

    @SuppressWarnings("unchecked")
    public synchronized Generator getGenerator(String generatorSpec) {

        Class<Generator> generatorClass = (Class<Generator>) resolveGeneratorClass(generatorSpec);
        Object[] generatorArgs = parseGeneratorArgs(generatorSpec);

        try {
            return ConstructorUtils.invokeConstructor(generatorClass, generatorArgs);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("unchecked")
    private Class<Generator> resolveGeneratorClass(String generatorSpec) {
        Class<Generator> generatorClass = null;
        String className = (generatorSpec.split(":"))[0];
        if (!className.contains(".")) {
            className = CycleNumberGenerator.class.getPackage().getName() + "." + className;
        }

        try {
            generatorClass = (Class<Generator>) Class.forName(className);
            logger.debug("HasInitialized class:" + generatorClass.getSimpleName() + " for generator type: " + generatorSpec);
            return generatorClass;
        } catch (ClassNotFoundException e) {
            logger.error("Unable to map generator class " + generatorSpec);
            throw new RuntimeException(e);
        }
    }

    private static Object[] parseGeneratorArgs(String generatorType) {
        String[] parts = generatorType.split(":");
        return Arrays.copyOfRange(parts, 1, parts.length);
    }

}
