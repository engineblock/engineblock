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

import com.google.common.collect.Lists;
import com.metawiring.load.generators.IntegerModSequenceGenerator;
import joptsimple.internal.Strings;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class GeneratorInstanceFactory<T> implements GeneratorFactory<T> {
    private final static Logger logger = LoggerFactory.getLogger(GeneratorInstanceFactory.class);

    private final String generatorSpec;

    private Class<Generator<T>> generatorClass;
    private Generator<T> theSharedInstance = null;
    private Object[] generatorArgs;
    private boolean isSharedInstance = false;

    public GeneratorInstanceFactory(String generatorSpec) {
        this.generatorSpec = generatorSpec;
        setLocal();
    }

//    public GeneratorInstanceFactory(Generator generator) {
//        this.theSharedInstance = generator;
//        this.generatorSpec="static:"+generator.getClass().getCanonicalName();
//        setShared();
//    }

    public GeneratorInstanceFactory(Class<? extends Generator> generatorClass, Object... constructorParams) {
        String conj = "";
        StringBuilder spec = new StringBuilder();
        spec.append(generatorClass.getCanonicalName()).append(":");
        for (Object o : constructorParams) {
            spec.append(conj); conj=":";
            spec.append(o.toString());
        }
        this.generatorSpec = spec.toString();
        setLocal();
    }

//    public GeneratorInstanceFactory setShared() {
//        this.isSharedInstance = true;
//        return this;
//    }

    public GeneratorFactory setLocal() {
        this.isSharedInstance = false;
        return this;
    }

    // TODO: Investigate issues with apache instantiation of classes with some static logic
    // when this method is not synchronized
    @Override
    public synchronized Generator<T> getGenerator() {

        if (isSharedInstance && theSharedInstance != null) {
            return theSharedInstance;
        }

        generatorClass = (Class<Generator<T>>) getGeneratorClass(generatorSpec);

        try {
            Generator<T> generator = ConstructorUtils.invokeConstructor(generatorClass, generatorArgs);
            if (isSharedInstance) {
                theSharedInstance = generator;
                return theSharedInstance;
            }
            return generator;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private Class<Generator<T>> getGeneratorClass(String generatorSpec) {
        if (generatorClass == null) {
            synchronized (this) {
                if (generatorClass != null) {
                    return generatorClass;
                }

                String className = (generatorSpec.split(":"))[0];
                if (!className.contains(".")) {
                    className = "com.metawiring.load.generators." + className;
                }

                try {
                    generatorClass = (Class<Generator<T>>) Class.forName(className);
                    logger.info("Initialized class:" + generatorClass.getSimpleName() + " for generator type: " + generatorSpec);
                } catch (ClassNotFoundException e) {
                    logger.error("Unable to map generator class " + generatorSpec + " for " + getClass().getSimpleName());
                    throw new RuntimeException(e);
                }

                generatorArgs = parseGeneratorArgs(generatorSpec);
            }
        }

        return generatorClass;
    }

    private Object[] parseGeneratorArgs(String generatorType) {
        String[] parts = generatorType.split(":");
        generatorArgs = Arrays.copyOfRange(parts, 1, parts.length);
        return generatorArgs;
    }

}
