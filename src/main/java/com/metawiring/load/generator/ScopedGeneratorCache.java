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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provide a cache of generator cache instances, with nested scope. Provides warnings or errors as requested if generators are already defined in an enclosing
 * scope. Allows scopes to be nested. Throws exceptions when scopes are not nested properly.
 * <p/>
 * Async Invariant: The cacheList must be kept in priority order, with more specific scopes listed first.
 */
public class ScopedGeneratorCache implements ScopedCachingGeneratorSource {
    private static Logger logger = LoggerFactory.getLogger(ScopedGeneratorCache.class);

    private RuntimeScope defaultGeneratorScope = RuntimeScope.activity;
    private LinkedList<ScopedGeneratorLevel> cacheList = new LinkedList<>();
    private final GeneratorInstanceSource generatorInstantiator;

    public ScopedGeneratorCache(
            GeneratorInstanceSource generatorInstantiator, RuntimeScope initialRuntimeScope) {
        this.generatorInstantiator = generatorInstantiator;
        this.cacheList.addFirst(new ScopedGeneratorLevel(initialRuntimeScope, generatorInstantiator));
    }


    @Override
    public ScopedGeneratorCache EnterSubScope(RuntimeScope subScope) {
        return new ScopedGeneratorCache(this, subScope);
    }

    private ScopedGeneratorCache(ScopedGeneratorCache parentCache, RuntimeScope subScope) {
        this.generatorInstantiator = parentCache.generatorInstantiator;
        RuntimeScope parentScope = parentCache.cacheList.peekFirst().runtimeScope;
        if (parentScope.hasHigherPrecedenceThan(subScope)) {
            throw new RuntimeException("CACHE-SCOPE-PRECEDENCE-ERROR: " + subScope + " must have higher precedence than parent scope: " + parentScope);
        }
        this.cacheList = Lists.newLinkedList(parentCache.cacheList);
        this.cacheList.addFirst(new ScopedGeneratorLevel(subScope, new CachingGeneratorSourceImpl(parentCache.generatorInstantiator)));
    }

    public RuntimeScope getRuntimeScope() {
        return cacheList.peekFirst().runtimeScope;
    }

    /**
     * Parses the specifiec scope from the front of the generatorSpec, and then does a lookup
     * within the named scope for a cached generator instance, optionally creating the cache layer if
     * configured to do so.
     * <p/>
     * <p>
     * The default scope is "activity"
     * </p>
     *
     * @param generatorSpec A string in "[scope] &lt;genspec&gt;" format.
     * @return a cached instance, possibly created at the scope level
     */
    @Override
    public Generator getGenerator(String generatorSpec) {
        ScopedGeneratorDef genSpec = new ScopedGeneratorDef(generatorSpec, defaultGeneratorScope);
        RuntimeScope targetScope = genSpec.runtimeScope;

        if (targetScope.hasEqualOrHigherPrecedenceThan(RuntimeScope.thread)) {
            // There is currently not a meaningful runtime precedence finer than thread, and
            // There is no need in exercizing the rest of the cache logic for thread scope or finer.
            Generator<?> generator = generatorInstantiator.getGenerator(genSpec.generatorSpec);
            return generator;
        }

        Iterator<ScopedGeneratorLevel> levelIterator = cacheList.iterator();
        GeneratorInstanceSource targetLevelCache = null;

        for (ScopedGeneratorLevel scopedGeneratorLevel : cacheList) {
            if (scopedGeneratorLevel.runtimeScope.compareTo(targetScope) == 0) {
                targetLevelCache = scopedGeneratorLevel.generatorSource;
                break;
            }
        }

        if (targetLevelCache == null) {
            throw new RuntimeException("CACHE-LEVEL-REFERENCE-ERROR: " + generatorSpec + ": No Such Cache " + ScopedGeneratorLevel.class.getSimpleName() + " for generator spec:" + generatorSpec);
        }

        Generator generator = targetLevelCache.getGenerator(genSpec.generatorSpec);
        return generator;
    }

    public static class ScopedGeneratorLevel implements Comparable<ScopedGeneratorLevel> {
        public RuntimeScope runtimeScope;
        public GeneratorInstanceSource generatorSource;

        public ScopedGeneratorLevel(RuntimeScope runtimeScope, GeneratorInstanceSource generatorSource) {
            this.runtimeScope = runtimeScope;
            this.generatorSource = generatorSource;
        }

        /**
         * This is in "precedence order", meaning that higher enum values will be placed earlier in the list.
         * This is the opposite of the enum order.
         */
        @Override
        public int compareTo(ScopedGeneratorLevel other) {
            return -this.runtimeScope.compareTo(other.runtimeScope);
        }

        @Override
        public String toString() {
            return "scope:" + runtimeScope + ", genSource:" + generatorSource;
        }
    }

    public static class ScopedGeneratorDef {
        private static Pattern scopeAndSpec = Pattern.compile("^\\s*((thread|activity|phase|test)?\\s+)?(.*)$");

        public RuntimeScope runtimeScope;
        public String generatorSpec;
        private RuntimeScope defaultScope;

        public ScopedGeneratorDef(String generatorSpec, RuntimeScope defaultScope) {
            this.defaultScope = defaultScope;
            parse(generatorSpec);
        }

        private void parse(String generatorSpec) {
            Matcher m = scopeAndSpec.matcher(generatorSpec);
            if (!m.matches()) {
                throw new RuntimeException("Unable to match generator spec with pattern: " + scopeAndSpec.pattern() + ", generator spec: " + generatorSpec);
            }
            MatchResult matchResult = m.toMatchResult();

            String genscope = matchResult.group(2);
            String genspec = matchResult.group(3);
            logger.trace("genscope:" + genscope + ", genspec" + genspec);

            if (matchResult.group(2) == null) {
                this.runtimeScope = defaultScope;
            } else {
                this.runtimeScope = RuntimeScope.valueOf(genscope);
            }

            this.generatorSpec = genspec;

        }
    }

}
