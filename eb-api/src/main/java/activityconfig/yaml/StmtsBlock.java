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

import activityconfig.MultiMapLookup;
import activityconfig.rawyaml.RawStmtsBlock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StmtsBlock implements Iterable<StmtDef> {

    private final RawStmtsBlock rawStmtsBlock;
    private final String blockName;
    private StmtsDoc rawStmtsDoc;
    private int blockIdx;


    public StmtsBlock(String blockName, RawStmtsBlock rawStmtsBlock, StmtsDoc rawStmtsDoc, int blockIdx) {
        this.blockName = blockName;
        this.rawStmtsBlock = rawStmtsBlock;
        this.rawStmtsDoc = rawStmtsDoc;
        this.blockIdx = blockIdx;
    }

    public List<StmtDef> getStmts() {
        List<StmtDef> rawStmtDefs = new ArrayList<>();

        List<String> statements = rawStmtsBlock.getStatements();
        for (int stmt = 0; stmt < statements.size(); stmt++) {
            String stmtName = getName() + "--" + (stmt + 1);
            String statement = statements.get(stmt);
            StmtDef stmtDef = new StmtDef(this,stmtName, statement);
            rawStmtDefs.add(stmtDef);
        }
        return rawStmtDefs;
    }

    public String getName() {
        StringBuilder sb = new StringBuilder();
        if (!rawStmtsDoc.getName().isEmpty()) {
            sb.append(rawStmtsDoc.getName()).append("--");
        }
        if (!rawStmtsBlock.getName().isEmpty()) {
            sb.append(rawStmtsBlock.getName());
        } else {
            sb.append("block").append(blockIdx);
        }
        return sb.toString();
    }

    public Map<String, String> getTags() {
        return new MultiMapLookup(rawStmtsBlock.getTags(), rawStmtsDoc.getTags());
    }

    public Map<String,String> getParams() {
        return new MultiMapLookup(rawStmtsBlock.getParams(), rawStmtsDoc.getParams());
    }

    public Map<String, String> getBindings() {
        return new MultiMapLookup(rawStmtsBlock.getBindings(), rawStmtsDoc.getBindings());
    }

    @NotNull
    @Override
    public Iterator<StmtDef> iterator() {
        return getStmts().iterator();
    }
}