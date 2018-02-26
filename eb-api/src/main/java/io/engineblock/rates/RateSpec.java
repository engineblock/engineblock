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

package io.engineblock.rates;

public class RateSpec {
    public double opsPerSec = 1.0D;
    public double strictness = 1.0D;

    public RateSpec(String spec) {
        String[] specs = spec.split("[,;]");
        switch (specs.length) {
            case 2:
                strictness = Double.valueOf(specs[1]);
            case 1:
                opsPerSec = Double.valueOf(specs[0]);
                break;
            default:
                throw new RuntimeException("Rate specs must be either '<rate>' or '<rate>:<strictness>' as in 5000.0 or 5000.0:1.0");
        }
    }

    public String toString() {
        return "opsPerSec:" + opsPerSec + ", strictness:" + strictness;
    }
}
