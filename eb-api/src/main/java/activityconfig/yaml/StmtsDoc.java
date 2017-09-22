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

import activityconfig.rawyaml.RawStmtsBlock;
import activityconfig.rawyaml.RawStmtsDoc;
import io.engineblock.util.Tagged;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AssembledStmtsDoc creates a logical view of a raw statements doc that includes
 * the overrides and filtering between the document layer and block layer.
 */
public class StmtsDoc implements Tagged {

    private RawStmtsDoc rawStmtsDoc;

    public StmtsDoc(RawStmtsDoc rawStmtsDoc) {
        this.rawStmtsDoc = rawStmtsDoc;
    }

    public List<StmtsBlock> getBlocks() {
        List<StmtsBlock> blocks = new ArrayList<>();

        int blockIdx=0;
        for (RawStmtsBlock rawStmtsBlock : rawStmtsDoc.getBlocks()) {
            String compositeName = rawStmtsDoc.getName() +
                    (rawStmtsBlock.getName().isEmpty() ? "" : "-" + rawStmtsBlock.getName());
            StmtsBlock compositeBlock = new StmtsBlock(compositeName, rawStmtsBlock,this,++blockIdx);
            blocks.add(compositeBlock);
        }

        return blocks;
    }

    @Override
    public Map<String, String> getTags() {
        return rawStmtsDoc.getTags();
    }

    public Map<String, String> getParams() {
        return rawStmtsDoc.getParams();
    }

    public Map<String, String> getBindings() {
        return rawStmtsDoc.getBindings();
    }

    public String getName() {
        return rawStmtsDoc.getName();
    }

}
