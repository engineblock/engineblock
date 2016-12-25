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

import activityconfig.yaml.AssembledStmtsBlock;
import activityconfig.yaml.StmtsBlock;
import activityconfig.yaml.StmtsDoc;
import io.engineblock.util.Tagged;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AssembledStmtsDoc creates a logical view of a raw statements doc that includes
 * the overrides and filtering between the document layer and block layer.
 */
public class AssembledStmtsDoc implements Tagged {

    private StmtsDoc rawStmtsDoc;

    public AssembledStmtsDoc(StmtsDoc rawStmtsDoc) {
        this.rawStmtsDoc = rawStmtsDoc;
    }

    public List<AssembledStmtsBlock> getAssembledBlocks() {
        List<AssembledStmtsBlock> assembledBlocks = new ArrayList<>();

        for (StmtsBlock rawStmtsBlock : rawStmtsDoc.getBlocks()) {

            AssembledStmtsBlock compositeBlock = new AssembledStmtsBlock(rawStmtsBlock);

            String compositeName = rawStmtsDoc.getName() +
                    (rawStmtsBlock.getName().isEmpty() ? "" : "-" + rawStmtsBlock.getName());
            compositeBlock.setName(compositeName);

            compositeBlock.setTags(new MultiMapLookup()
                    .add(rawStmtsBlock.getTags())
                    .add(rawStmtsDoc.getTags())
            );

            compositeBlock.setConfig(new MultiMapLookup()
                    .add(rawStmtsBlock.getConfig())
                    .add(rawStmtsDoc.getConfig())
            );

            compositeBlock.setBindings(new MultiMapLookup()
                    .add(rawStmtsBlock.getBindings())
                    .add(rawStmtsDoc.getBindings())
            );

            assembledBlocks.add(compositeBlock);

        }

        return assembledBlocks;
    }

    @Override
    public Map<String, String> getTags() {
        return rawStmtsDoc.getTags();
    }

    public String getName() {
        return rawStmtsDoc.getName();
    }
}
