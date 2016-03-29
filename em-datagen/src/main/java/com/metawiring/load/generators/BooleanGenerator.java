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

package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;
import com.metawiring.load.generator.ThreadsafeGenerator;

public class BooleanGenerator implements Generator<Boolean>,ThreadsafeGenerator {

    private Boolean boolValue;

    public BooleanGenerator(String boolValue) {
        this(Boolean.valueOf(boolValue));
    }
    public BooleanGenerator(Boolean boolValue) {
        this.boolValue = boolValue;
    }

    @Override
    public Boolean get() {
        return boolValue;
    }
}
