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

/**
 * A statements doc can have either a list of statement blocks or a
 * list of statements but not both. It can also have all the block
 * parameters assignable to {@link BlockParams}.
 * <p>
 * The reason for having support both statements or statement blocks
 * is merely convenience. If you do not need or want to deal with the
 * full blocks format, the extra structure gets in the way. However,
 * having both a list and a blocks section in the same document can
 * create confusion. The compromise was to allow for either, but
 * specifically disallow them together.
 */
public class StmtsDoc extends BlockParams {

    private List<StmtsBlock> blocks = new ArrayList<>();
    private List<String> statements = new ArrayList<>();

    public List<String> getStatements() {
        return statements;
    }

    public void setStatements(List<String> statements) {

        if (!blocks.isEmpty()) {
            throw new RuntimeException("presently, you must pick between having " +
                    "blocks and statement at the document level. This on already has blocks." +
                    " Add your statements under it.");
        }
        this.statements.clear();
        this.statements.addAll(statements);
    }

    /**
     * Return the list of statement blocks in this StmtsDoc.
     * If raw statements are defined on this StmtsDoc, then a single
     * StmtBlock containing those statements is returned. Otherwise,
     * the list of StmtBlocks is returned.
     *
     * @return all logical statement blocks containing statements
     */
    public List<StmtsBlock> getBlocks() {
        if (!statements.isEmpty()) {
            return new ArrayList<StmtsBlock>() {{
                add(new StmtsBlock(statements));
            }};
        } else {
            return blocks;
        }
    }

    public void setBlocks(List<StmtsBlock> blocks) {
        if (!statements.isEmpty()) {
            throw new RuntimeException("presently, you must pick between having " +
                    "blocks and statement at the document level." +
                    " This one already has statements. You can move " +
                    "them to a new blocks section");
        }
        this.blocks.clear();
        this.blocks.addAll(blocks);
    }
}
