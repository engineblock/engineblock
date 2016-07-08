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

import io.engineblock.activityapi.ActivityDef;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class StrInterpolater implements Function<String, String> {

    private MultiMap multimap = new MultiMap();
    private StrSubstitutor substitutor= new StrSubstitutor(multimap,"<<",">>",'\\',",");

    public StrInterpolater(ActivityDef... activityDefs) {
        Arrays.asList(activityDefs).stream()
                .map(ad -> ad.getParams().getStringStringMap())
                .forEach(multimap::add);
    }

    // for testing
    protected StrInterpolater(List<Map<String, String>> maps) {
        maps.forEach(multimap::add);
    }

    @Override
    public String apply(String s) {
        return substitutor.replace(s);
    }

    private static class MultiMap extends StrLookup<String> {

        private List<Map<String, String>> maps = new ArrayList<>();
        private String warnPrefix = "UNSET";

        public void add(Map<String,String> addedMap) {
            maps.add(addedMap);
        }

        @Override
        public String lookup(String key) {
            String defval=null;

            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                key = parts[0];
                defval = parts[1];
            }

            for (Map<String, String> map : maps) {
                String val = map.get(key);
                if (val != null) {
                    return val;
                }
            }

            return (defval != null) ? defval : warnPrefix + ":" + key;
        }
    }

}
