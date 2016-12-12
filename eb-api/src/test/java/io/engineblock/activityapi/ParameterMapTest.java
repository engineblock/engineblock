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

package io.engineblock.activityapi;

import io.engineblock.activityimpl.ParameterMap;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.util.Optional;
public class ParameterMapTest {

    @Test
    public void testNullStringYieldsNothing() {
        Optional<ParameterMap> parameterMap = ParameterMap.parseParams(null);
        Assertions.assertThat(parameterMap.isPresent()).isFalse();
    }

    @Test
    public void testEmptyStringYieldsEmptyMap() {
        Optional<ParameterMap> parameterMap = ParameterMap.parseParams("");
        Assertions.assertThat(parameterMap.isPresent()).isTrue();
    }

    @Test
    public void testUnparsableYieldsNothing() {
        Optional<ParameterMap> unparseable = ParameterMap.parseParams("woejfslkdjf");
        Assertions.assertThat(unparseable.isPresent()).isFalse();

    }

    @Test
    public void testGetLongParam() {
        Optional<ParameterMap> longOnly = ParameterMap.parseParams("longval=234433;");
        Assertions.assertThat(longOnly.isPresent()).isTrue();
        Assertions.assertThat(longOnly.get().getOptionalLong("longval").orElse(12345L)).isEqualTo(234433L);
        Assertions.assertThat(longOnly.get().getOptionalLong("missing").orElse(12345L)).isEqualTo(12345L);
    }

    @Test
    public void testGetDoubleParam() {
        Optional<ParameterMap> doubleOnly = ParameterMap.parseParams("doubleval=2.34433;");
        Assertions.assertThat(doubleOnly.isPresent()).isTrue();
        Assertions.assertThat(doubleOnly.get().getOptionalDouble("doubleval").orElse(3.4567d)).isEqualTo(2.34433d);
        Assertions.assertThat(doubleOnly.get().getOptionalDouble("missing").orElse(3.4567d)).isEqualTo(3.4567d);
    }

    @Test
    public void testGetStringParam() {
        Optional<ParameterMap> stringOnly = ParameterMap.parseParams("stringval=avalue;");
        Assertions.assertThat(stringOnly.isPresent()).isTrue();
        Assertions.assertThat(stringOnly.get().getOptionalString("stringval").orElse("othervalue")).isEqualTo("avalue");
        Assertions.assertThat(stringOnly.get().getOptionalString("missing").orElse("othervalue")).isEqualTo("othervalue");
    }

    @Test
    public void testGetStringStringParam() {
        Optional<ParameterMap> stringOnly = ParameterMap.parseParams("stringval=avalue;stringval2=avalue2;");
        Assertions.assertThat(stringOnly.isPresent()).isTrue();
        Assertions.assertThat(stringOnly.get().getOptionalString("stringval").orElse("othervalue")).isEqualTo("avalue");
        Assertions.assertThat(stringOnly.get().getOptionalString("stringval2").orElse("othervalue1")).isEqualTo("avalue2");
    }

    @Test
    public void testGetOptional() {
        ParameterMap abc = ParameterMap.parseOrException("a=1;b=2;c=3;");
        Optional<Long> d = abc.getOptionalLong("d");
        Assertions.assertThat(d).isEmpty();
        Optional<String> a = abc.getOptionalString("a");
        Assertions.assertThat(a).isEqualTo(Optional.of("1"));
        Optional<Long> aLong = abc.getOptionalLong("a");
        Assertions.assertThat(aLong).isEqualTo(Optional.of(1L));
    }

    @Test
    public void testQuotedSemis() {
        ParameterMap abc = ParameterMap.parseOrException("a=1;b='two;three';");
    }
}