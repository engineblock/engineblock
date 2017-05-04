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
package io.engineblock.activities.json.statements;

import io.virtdata.core.Bindings;
import java.util.List;

public class ReadyFileMapStatement implements ReadyFileStatement {
    private String statementTemplate;
    private Bindings dataBindings;

    public ReadyFileMapStatement(String statementTemplate, Bindings dataBindings) {
        this.statementTemplate = statementTemplate;
        this.dataBindings = dataBindings;
    }

    public String bind(long cycleNum) {
        String statement = null;
        int i=0;
        //for json we will need the names not just the values
        List<String> allNames = dataBindings.getTemplate().getBindPointNames();
        for (String name : allNames) {
            if (statement != null) {
                statement = statement + "," + dataBindings.get(i, cycleNum);
            }
            else {
                statement = (String) dataBindings.get(i, cycleNum);
            }
            i++;
        }
        return statement;
    }

}
