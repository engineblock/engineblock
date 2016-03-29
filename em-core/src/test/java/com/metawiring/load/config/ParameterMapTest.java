package com.metawiring.load.config;

import com.metawiring.load.activityapi.ParameterMap;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(parameterMap.isPresent()).isFalse();
    }

    @Test
    public void testEmptyStringYieldsEmptyMap() {
        Optional<ParameterMap> parameterMap = ParameterMap.parseParams("");
        assertThat(parameterMap.isPresent()).isTrue();
    }

    @Test
    public void testUnparsableYieldsNothing() {
        Optional<ParameterMap> unparseable = ParameterMap.parseParams("woejfslkdjf");
        assertThat(unparseable.isPresent()).isFalse();

    }

    @Test
    public void testGetLongParam() {
        Optional<ParameterMap> longOnly = ParameterMap.parseParams("longval=234433;");
        assertThat(longOnly.isPresent()).isTrue();
        assertThat(longOnly.get().getLongOrDefault("longval", 12345L)).isEqualTo(234433L);
        assertThat(longOnly.get().getLongOrDefault("missing", 12345L)).isEqualTo(12345L);
    }

    @Test
    public void testGetDoubleParam() {
        Optional<ParameterMap> doubleOnly = ParameterMap.parseParams("doubleval=2.34433;");
        assertThat(doubleOnly.isPresent()).isTrue();
        assertThat(doubleOnly.get().getDoubleOrDefault("doubleval", 3.4567d)).isEqualTo(2.34433d);
        assertThat(doubleOnly.get().getDoubleOrDefault("missing", 3.4567d)).isEqualTo(3.4567d);
    }

    @Test
    public void testGetStringParam() {
        Optional<ParameterMap> stringOnly = ParameterMap.parseParams("stringval=avalue;");
        assertThat(stringOnly.isPresent()).isTrue();
        assertThat(stringOnly.get().getStringOrDefault("stringval", "othervalue")).isEqualTo("avalue");
        assertThat(stringOnly.get().getStringOrDefault("missing", "othervalue")).isEqualTo("othervalue");
    }

    @Test
    public void testGetStringStringParam() {
        Optional<ParameterMap> stringOnly = ParameterMap.parseParams("stringval=avalue;stringval2=avalue2;");
        assertThat(stringOnly.isPresent()).isTrue();
        assertThat(stringOnly.get().getStringOrDefault("stringval", "othervalue")).isEqualTo("avalue");
        assertThat(stringOnly.get().getStringOrDefault("stringval2", "othervalue1")).isEqualTo("avalue2");
    }


    @Test
    public void testPositionalParsing() {
        ParameterMap matchingNumbers = ParameterMap.parsePositional("1;2", new String[]{"one", "two", "three"});
        assertThat(matchingNumbers.getSize()).isEqualTo(2);
        assertThat(matchingNumbers.getIntOrDefault("one", 5)).isEqualTo(1);
        assertThat(matchingNumbers.getStringOrDefault("two", "default")).isEqualTo("2");
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
        assertThat(changeCounter.get()).isEqualTo(0L);
        assertThat(matchingNumbers.getDoubleOrDefault("three", 4.0D)).isEqualTo(3.0D);
        matchingNumbers.set("three", 2.7777D);
        assertThat(changeCounter.get()).isEqualTo(1L);
        assertThat(matchingNumbers.getDoubleOrDefault("three", 9.8D)).isEqualTo(2.7777D);
        matchingNumbers.set("three", "seventeen");
        assertThat(changeCounter.get()).isEqualTo(2L);
        assertThat(matchingNumbers.getStringOrDefault("three", "whoops")).isEqualTo("seventeen");
    }

    @Test
    public void testGetOptional() {
        ParameterMap abc = ParameterMap.parseOrException("a=1;b=2;c=3;");
        Optional<Long> d = abc.getOptionalLong("d");
        assertThat(d).isEmpty();
        Optional<String> a = abc.getOptionalString("a");
        assertThat(a).isEqualTo(Optional.of("1"));
        Optional<Long> aLong = abc.getOptionalLong("a");
        assertThat(aLong).isEqualTo(Optional.of(1L));
    }
}