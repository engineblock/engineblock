/*
*   Copyright 2017 jshook
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
package io.engineblock.activities.csv.statements;

import io.virtdata.core.Bindings;

public class ReadyCSVMapStatement implements ReadyCSVStatement {
    private String statementTemplate;
    private Bindings dataBindings;

    public ReadyCSVMapStatement(String statementTemplate, Bindings dataBindings) {
        this.statementTemplate = statementTemplate;
        this.dataBindings = dataBindings;
    }

    public String bind(long cycleNum) {
        StringConcatSetter setter = new StringConcatSetter();
        dataBindings.setFields(setter,cycleNum);
        return dataBindings.toString();
    }

    /**
     * This private method is responsible for handling how field assignments (field name and value)
     * are done within {@link ReadyFileMapStatement}
     */
    private static class StringConcatSetter implements Bindings.FieldSetter {
        private StringBuilder sb = new StringBuilder();

        @Override
        public void setField(String name, Object value) {
            if (sb.length()>0) {
                sb.append(",");
            }
            sb.append(value);
        }

        public String toString() {
            return sb.toString();
        }
    }

}
