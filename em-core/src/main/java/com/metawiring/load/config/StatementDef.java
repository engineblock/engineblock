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

package com.metawiring.load.config;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatementDef {
    public String name = "";
    public String cql = "";
    public Map<String, String> bindings = new HashMap<>();

    public StatementDef() {
    }

    public StatementDef(String name, String cql, Map<String,String> bindings) {
        this.name = name;
        this.cql = cql;
        this.bindings = bindings;
    }

    Pattern bindableRegex = Pattern.compile("<<(\\w+)>>");

    /**
     * @return bindableNames in order as specified in the parameter placeholders
     */
    public List<String> getBindNames() {
        Matcher m = bindableRegex.matcher(cql);
        List<String> bindNames = new ArrayList<>();
        while (m.find()) {
            bindNames.add(m.group(1));
        }
        return bindNames;
    }

    public List<String> getBindNamesExcept(String... exceptNames) {
        Set<String> exceptNamesSet = new HashSet<>();
        Arrays.asList(exceptNames).stream().map(String::toLowerCase).forEach(exceptNamesSet::add);
        List<String> names = new ArrayList<>();
        List<String> allBindNames = getBindNames();
        allBindNames.stream().filter(s -> !exceptNamesSet.contains(s.toLowerCase())).forEach(names::add);
        return names;
    }

    /**
     * @param config The parameters which may be needed to qualify token substitutions.
     *               <ul>
     *               <li>keyspace</li>
     *               <li>table</li>
     *               <li>rf</li>
     *               </ul>
     * @return CQL statement with '?' in place of the bindable parameters, suitable for preparing
     */
    public String getCookedStatement(ParameterMap config) {
        String statement = cql;
        statement = statement.replaceAll("<<KEYSPACE>>", config.getStringOrDefault("keyspace", "default"));
        statement = statement.replaceAll("<<TABLE>>", config.getStringOrDefault("table", "default"));
        statement = statement.replaceAll("<<RF>>", String.valueOf(config.getStringOrDefault("rf", "1")));
        statement = statement.replaceAll("<<\\w+>>", "?");
        return statement;
    }
}
