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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * See TagFilterTest for details on this class.
 */
public class TagFilter {
    private Map<String, String> filterby = new HashMap<>();

    public TagFilter(String tagSpec) {
        if ((tagSpec != null) && (!tagSpec.isEmpty())) {
            String[] keyvalues = tagSpec.split("[, ]");
            for (String assignment : keyvalues) {
                String[] keyvalue = assignment.split("[:=]", 2);
                String key = keyvalue[0];
                String value = keyvalue.length > 1 ? keyvalue[1] : null;
                if (value!=null
                        && ((value.indexOf("\'") == 0) && ((value.indexOf("\'", 1) == (value.length() - 1))))) {
                    value = value.substring(1, value.length() - 1);
                }
                filterby.put(key, value);
            }
        }
    }

    protected boolean matches(Map<String, String> itemTags) {

        // if no item tags were requested, then everything matches
        if (filterby.size() == 0) {
            return true;
        }

        // if filtering tags were requested, but no item tags exists, then nothing matches
        if (itemTags.size() == 0) {
            return false;
        }

        for (String filterkey : filterby.keySet()) {
            String filterval = filterby.get(filterkey);
            Pattern filterPattern = Pattern.compile("^" + filterval + "$");

            String itemval = itemTags.get(filterkey);

            boolean keymatch = (
                    itemTags.containsKey(filterkey) &&
                            (filterval == null || filterPattern.matcher(itemval).matches()));

            if (!keymatch) {
                return false;
            }
        }
        return true;
    }

    public boolean matches(Tagged item) {
        Map<String, String> itemTags = item.getTags();
        return matches(itemTags);
    }

    public Map<String, String> getMap() {
        return filterby;
    }
}
