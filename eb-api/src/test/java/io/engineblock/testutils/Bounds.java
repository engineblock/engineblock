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

package io.engineblock.testutils;

public class Bounds {
    private final int levelsPerMagnitude;
    private long currentValue;

    public Bounds(long startingValue, int levelsPerMagnitude) {
        this.currentValue=startingValue;
        this.levelsPerMagnitude = levelsPerMagnitude;
    }

    public Bounds setValue(long value) {
        this.currentValue = value;
        return this;
    }

    public long getValue() {
        return currentValue;
    }

    public long getNextValue() {
        long nextValue = findNextHigherValue();
        currentValue=nextValue;
        return currentValue;
    }

    private long findNextHigherValue() {
        int pow10 = (int) Math.log10(currentValue);
        if (levelsPerMagnitude==1) {
            return (long) Math.pow(10,pow10+1);
        }
        double baseMagnitude = Math.pow(10, pow10);
        double increment = baseMagnitude/ levelsPerMagnitude;

        long newValue = (long) (currentValue + increment);
        return newValue;
    }

    @Override
    public String toString() {
        return String.valueOf(this.currentValue) + "(incr by 1/" + this.levelsPerMagnitude + ")";
    }
}
