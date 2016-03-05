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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create generator instances from generator specs with scoping rules.
 * <ul>
 *     <li>Generator references may start with "thread" or "activity" and a whitespace.</li>
 *     <li>Generators which are not annotated as threadsafe may not be used in "activity" references.</li>
 *     <li>Generators references which are not marked neither "thread" nor "activity" are presumed to
 *      be "thread", but generate a warning in the logs.
 * </ul>
 *
 * This class may hold references to objects in order to provide an effective cache for cross-thread instances.
 * You must dereference this object when it is no longer needed in order to allow the heap to be cleaned once
 * all client threads are done as well.
 */
public class ScopedGeneratorInstanceFactory  {
}
