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

package io.engineblock.util;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class StrInterpolaterTest {

    private static List<Map<String, String>> abcd = new ArrayList<Map<String, String>>() {{
        add(
                new HashMap<String,String>() {{
                    put("akey", "aval1");
                    put("bkey", "bval1");
                    put("ckey", "cval1");
                }}
        );
        add(
                new HashMap<String,String>() {
                    {
                        put("akey", "aval2");
                        put("bkey", "bval2");
                    }
                }
        );
    }};

    private static StrInterpolater interp = new StrInterpolater(abcd);

    @Test
    public void shouldReturnIdentity() {
        String a = interp.apply("A");
        assertThat(a).isEqualTo("A");
    }

    @Test
    public void shouldMatchSimpleSubst() {
        String a = interp.apply("<<akey>>");
        assertThat(a).isEqualTo("aval1");
    }

    @Test
    public void shouldReturnWarningWhenUnmatched() {
        String a = interp.apply("<<nokeymatchesthis>>");
        assertThat(a).isEqualTo("UNSET:nokeymatchesthis");
    }

    @Test
    public void shouldReturnDefaultWhenNotOverridden() {
        String a = interp.apply("<<nokeymatchesthis:butithasadefault>>");
        assertThat(a).isEqualTo("butithasadefault");
    }

    @Test
    public void shouldOverrideDefault() {
        String a = interp.apply("<<bkey:bkeydefault>>");
        assertThat(a).isEqualTo("bval1");
    }

}