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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StrInterpolater implements Function<String, String> {

    private StrSubstitutor substitutor;
    private MultiMap multimap;

    public StrInterpolater(ActivityDef... activityDefs) {
        ArrayList<Map<String, String>> listOfMaps = Arrays.asList(activityDefs).stream()
                .map(ad -> ad.getParams().getStringStringMap())
                .collect(Collectors.toCollection(ArrayList::new));

        this.multimap = new MultiMap(listOfMaps, "INTERPOLATION_ERROR");
    }

    // for testing
    protected StrInterpolater(Map<String, String> map, String warnPrefix) {
        this(new ArrayList<Map<String, String>>() {{
            add(map);
        }}, warnPrefix);
    }

    // for testing
    protected StrInterpolater(List<Map<String, String>> maps, String warnPrefix) {
        this.multimap = new MultiMap(maps, warnPrefix);
        this.substitutor = new StrSubstitutor(multimap, "<<", ">>", '\\', ",");
    }

    @Override
    public String apply(String s) {
        return substitutor.replace(s);
    }

    private static class MultiMap extends StrLookup<String> {

        private List<Map<String, String>> maps;
        private String warnPrefix = "UNSET:";

        public MultiMap(List<Map<String, String>> maps, String warnPrefix) {
            this.maps = maps;
            this.warnPrefix = warnPrefix;
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
