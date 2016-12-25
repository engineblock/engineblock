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

package activityconfig.yaml;

import java.util.ArrayList;
import java.util.List;

public class AssembledStmtsBlock extends BlockParams {

    private final StmtsBlock rawStmtsBlock;

    public AssembledStmtsBlock(StmtsBlock rawStmtsBlock) {
        this.rawStmtsBlock = rawStmtsBlock;
    }

    public List<StatementDef> getAssembledStatements() {
        List<StatementDef> statementDefs = new ArrayList<>();

        List<String> statements = rawStmtsBlock.getStatements();
        for (int stmt = 0; stmt < statements.size(); stmt++) {
            String stmtName = getName() + "-" + (stmt + 1);
            StatementDef statementDef = new StatementDef(stmtName,statements.get(stmt));
            statementDefs.add(statementDef);
        }
        return statementDefs;
    }

}