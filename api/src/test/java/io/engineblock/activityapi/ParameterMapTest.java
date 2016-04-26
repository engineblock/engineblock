package io.engineblock.activityapi;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/*
*   Copyright 2015 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
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
        Assertions.assertThat(longOnly.get().getLongOrDefault("longval", 12345L)).isEqualTo(234433L);
        Assertions.assertThat(longOnly.get().getLongOrDefault("missing", 12345L)).isEqualTo(12345L);
    }

    @Test
    public void testGetDoubleParam() {
        Optional<ParameterMap> doubleOnly = ParameterMap.parseParams("doubleval=2.34433;");
        Assertions.assertThat(doubleOnly.isPresent()).isTrue();
        Assertions.assertThat(doubleOnly.get().getDoubleOrDefault("doubleval", 3.4567d)).isEqualTo(2.34433d);
        Assertions.assertThat(doubleOnly.get().getDoubleOrDefault("missing", 3.4567d)).isEqualTo(3.4567d);
    }

    @Test
    public void testGetStringParam() {
        Optional<ParameterMap> stringOnly = ParameterMap.parseParams("stringval=avalue;");
        Assertions.assertThat(stringOnly.isPresent()).isTrue();
        Assertions.assertThat(stringOnly.get().getStringOrDefault("stringval", "othervalue")).isEqualTo("avalue");
        Assertions.assertThat(stringOnly.get().getStringOrDefault("missing", "othervalue")).isEqualTo("othervalue");
    }

    @Test
    public void testGetStringStringParam() {
        Optional<ParameterMap> stringOnly = ParameterMap.parseParams("stringval=avalue;stringval2=avalue2;");
        Assertions.assertThat(stringOnly.isPresent()).isTrue();
        Assertions.assertThat(stringOnly.get().getStringOrDefault("stringval", "othervalue")).isEqualTo("avalue");
        Assertions.assertThat(stringOnly.get().getStringOrDefault("stringval2", "othervalue1")).isEqualTo("avalue2");
    }


    @Test
    public void testPositionalParsing() {
        ParameterMap matchingNumbers = ParameterMap.parsePositional("1;2", new String[]{"one", "two", "three"});
        Assertions.assertThat(matchingNumbers.getSize()).isEqualTo(2);
        Assertions.assertThat(matchingNumbers.getIntOrDefault("one", 5)).isEqualTo(1);
        Assertions.assertThat(matchingNumbers.getStringOrDefault("two", "default")).isEqualTo("2");
    }

    @Test(expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = ".*ran out of positional field names.*")
    public void testFieldNameUnderrun() {
        ParameterMap underrun = ParameterMap.parsePositional("1;2;3;4;", new String[]{"one", "two", "three"});
    }

    @Test
    public void testSetSignatures() {
        ParameterMap matchingNumbers = ParameterMap.parsePositional("1;2;3.0;", new String[]{"one", "two", "three"});
        AtomicLong changeCounter = matchingNumbers.getChangeCounter();
        Assertions.assertThat(changeCounter.get()).isEqualTo(0L);
        Assertions.assertThat(matchingNumbers.getDoubleOrDefault("three", 4.0D)).isEqualTo(3.0D);
        matchingNumbers.set("three", 2.7777D);
        Assertions.assertThat(changeCounter.get()).isEqualTo(1L);
        Assertions.assertThat(matchingNumbers.getDoubleOrDefault("three", 9.8D)).isEqualTo(2.7777D);
        matchingNumbers.set("three", "seventeen");
        Assertions.assertThat(changeCounter.get()).isEqualTo(2L);
        Assertions.assertThat(matchingNumbers.getStringOrDefault("three", "whoops")).isEqualTo("seventeen");
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
}