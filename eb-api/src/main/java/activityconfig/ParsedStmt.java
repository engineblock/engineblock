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

package activityconfig;

import activityconfig.yaml.StmtDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Allow for uniform statement anchor parsing, using the <pre>?anchor</pre>
 * and <pre>{anchor}</pre> anchoring conventions. This type also includes
 * all of the properties from the enclosed StmtDef, in addition to a couple of
 * helpers. It should allow programmers to project this type directly from an
 * existing {@link StmtDef} as a substitute.
 */
public class ParsedStmt {

    private final static Pattern stmtToken = Pattern.compile("\\?(\\w+[-_\\d\\w]*)|\\{(\\w+[-_\\d\\w.]*)}");
    private final static Logger logger = LoggerFactory.getLogger(ParsedStmt.class);

    private final StmtDef stmtDef;
    private final String[] spans;
    private final Set<String> missingBindings = new HashSet<>();
    private final Set<String> extraBindings = new HashSet<>();
    private final Map<String, String> specificBindings = new LinkedHashMap<>();
    private String anchor;

    /**
     * Construct a new ParsedStatement from the provided stmtDef and anchor token.
     *
     * @param stmtDef An existing statement def as read from the YAML API.
     */
    public ParsedStmt(StmtDef stmtDef) {
        this.stmtDef = stmtDef;
        this.spans = parse();
    }

    public ParsedStmt orError() {
        if (hasError()) {
            throw new RuntimeException("Unable to parse statement: " + this.toString());
        }
        return this;
    }

    private String[] parse() {
        List<String> spans = new ArrayList<>();

        extraBindings.addAll(stmtDef.getBindings().keySet());

        String statement = stmtDef.getStmt();

        Matcher m = stmtToken.matcher(statement);
        int lastMatch = 0;
        String remainder = "";
        while (m.find(lastMatch)) {
            String pre = statement.substring(lastMatch, m.start());

            String form1 = m.group(1);
            String form2 = m.group(2);
            String tokenName = (form1 != null && !form1.isEmpty()) ? form1 : form2;
            lastMatch = m.end();
            spans.add(pre);

            if (extraBindings.contains(tokenName)) {
                if (specificBindings.get(tokenName) != null){
                    String postfix = UUID.randomUUID().toString();
                    specificBindings.put(tokenName+postfix, stmtDef.getBindings().get(tokenName));
                }else {
                    specificBindings.put(tokenName, stmtDef.getBindings().get(tokenName));
                }
            } else {
                missingBindings.add(tokenName);
            }
        }
        specificBindings.forEach((k,v) -> extraBindings.remove(k));

        if (lastMatch >= 0) {
            spans.add(statement.substring(lastMatch));
        } else {
            spans.add(statement);
        }

        return spans.toArray(new String[0]);
    }

    public String toString() {
        String summary =
                (this.missingBindings.size() > 0) ?
                        "\nundefined bindings:" + this.missingBindings.stream().collect(Collectors.joining(",", "[", "]")) : "";
        return "STMT:" + stmtDef.getStmt() + "\n" + summary;
    }

    /**
     * @return true if the parsed statement is not usable.
     */
    public boolean hasError() {
        return missingBindings.size() > 0;
    }

    /**
     * The list of binding names returned by this method does not
     * constitute an error. They may be used for
     * for informational purposes in error handlers, for example.
     *
     * @return a set of bindings names which were provided to
     * this parsed statement, but which were not referenced
     * in either <pre>{anchor}</pre> or <pre>?anchor</pre> form.
     */
    public Set<String> getExtraBindings() {
        return extraBindings;
    }

    /**
     * Returns a list of binding names which were referenced
     * in either <pre>{anchor}</pre> or <pre>?anchor</pre> form,
     * but which were not present in the provided bindings map.
     * If any binding names are present in the returned set, then
     * this binding will not be usable.
     *
     * @return A list of binding names which were referenced but not defined*
     */
    public Set<String> getMissingBindings() {
        return missingBindings;
    }

    /**
     * Return a map of bindings which were referenced in the statement.
     * This is an easy way to get the list of effective bindings for
     * a statement for diagnostic purposes without including a potentially
     * long list of library bindings.
     * @return a bindings map of referenced bindings in the statement
     */
    public Map<String, String> getSpecificBindings() {
        return specificBindings;
    }

    /**
     * Return the statement that can be used as-is by any driver specific version.
     * This uses the anchor token as provided to yield a version of the statement
     * which contains positional anchors, but no named bindings.
     * @param anchorToken The token which is to be used as a positional place holder
     * @return A driver or usage-specific format of the statement, with anchors
     */
    public String getPositionalStatement(String anchorToken) {
        StringBuilder sb = new StringBuilder(spans[0]);
        for (int i = 1; i < spans.length; i++) {
            sb.append(anchorToken).append(spans[i]);
        }
        return sb.toString();
    }

    /**
     * @return the statement name from the enclosed {@link StmtDef}
     */
    public String getName() {
        return stmtDef.getName();
    }

    /**
     * @return the raw statement from the enclosed {@link StmtDef}
     */
    public String getStmt() {
        return stmtDef.getStmt();
    }

    /**
     * @return the tags from the enclosed {@link StmtDef}
     */
    public Map<String, String> getTags() {
        return stmtDef.getTags();
    }

    /**
     * @return the bindings from the enclosed {@link StmtDef}
     */
    public Map<String, String> getBindings() {
        return stmtDef.getBindings();
    }

    /**
     * @return the params from the enclosed {@link StmtDef}
     */
    public Map<String, String> getParams() {
        return stmtDef.getParams();
    }

}
