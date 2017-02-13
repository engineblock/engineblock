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

import activityconfig.AssembledStmtsDoc;
import activityconfig.AssembledStmtsDocList;
import activityconfig.Statements;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class StatementDefsTest {

    @Test
    public void testLayering() {
        AssembledStmtsDocList all = Statements.load("testdocs/everything.yaml");
        assertThat(all).isNotNull();
        assertThat(all.getAssembledStmtsDocs()).hasSize(2);
        AssembledStmtsDoc doc1 = all.getAssembledStmtsDocs().get(0);
        assertThat(doc1.getName()).isEqualTo("name1");
        assertThat(doc1.getAssembledBlocks()).hasSize(1);
        AssembledStmtsDoc doc2 = all.getAssembledStmtsDocs().get(1);
        assertThat(doc2.getAssembledBlocks()).hasSize(2);

        AssembledStmtsBlock block1 = doc1.getAssembledBlocks().get(0);
        assertThat(block1.getBindings()).hasSize(2);
        assertThat(block1.getName()).isEqualTo("name1--block1");
        assertThat(block1.getTags()).hasSize(1);

        AssembledStmtsBlock block21 = doc2.getAssembledBlocks().get(0);
        AssembledStmtsBlock block22 = doc2.getAssembledBlocks().get(1);

        assertThat(block21.getName()).isEqualTo("name2--block1");
        assertThat(block21.getTags()).hasSize(3);

        assertThat(block22.getName()).isEqualTo("name2--block2");
        assertThat(block22.getTags()).hasSize(2);
        assertThat(block22.getTags().get("root1")).isEqualTo("value23");
    }

    @Test
    public void testStatementRendering() {
        AssembledStmtsDocList all = Statements.load("testdocs/everything.yaml");
        assertThat(all).isNotNull();
        assertThat(all.getAssembledStmtsDocs()).hasSize(2);
        AssembledStmtsDoc doc1 = all.getAssembledStmtsDocs().get(0);
        AssembledStmtsBlock block1 = doc1.getAssembledBlocks().get(0);
        assertThat(block1.getName()).isEqualTo("name1--block1");
        List<StatementDef> assys = block1.getAssembledStatements();
        assertThat(assys).hasSize(2);
        StatementDef sdef1 = assys.get(0);
        assertThat(sdef1.getName()).isEqualTo("name1--block1--1");
        assertThat(assys.get(0).getStatement()).isEqualTo("s1");

    }

}