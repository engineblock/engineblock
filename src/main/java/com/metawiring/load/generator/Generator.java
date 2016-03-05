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

public interface Generator<T> {

    /**
     * Return a sample.
     * @return generated "sample" of parameterized type T
     */
    public T get();

    /**
     * For these classes, toString is the same as the specifier string that is used to map the
     * implementation and parameters. That is, you should be able take this value and put in
     * a configuration file to reproduce the exact configured generator instance
     * @return spec string
     */
    public String toString();
}
