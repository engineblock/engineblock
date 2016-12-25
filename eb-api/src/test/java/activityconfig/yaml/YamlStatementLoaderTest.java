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

import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class YamlStatementLoaderTest {

    @Test
    public void tetLoadPropertiesBlock() {
        YamlStatementLoader ysl = new YamlStatementLoader();
        StmtsDocList rawBlockDocs = ysl.load("testdocs/rawblock.yaml");
        assertThat(rawBlockDocs.getStmtsDocs()).hasSize(1);
        StmtsDoc rawBlockDoc = rawBlockDocs.getStmtsDocs().get(0);
        assertThat(rawBlockDoc.getStatements()).hasSize(1);
        assertThat(rawBlockDoc.getBindings()).hasSize(1);
        assertThat(rawBlockDoc.getName()).isEqualTo("name");
        assertThat(rawBlockDoc.getTags()).hasSize(1);
        assertThat(rawBlockDoc.getConfig()).hasSize(1);
    }

    @Test
    public void testLoadFullFormat() {
        YamlStatementLoader ysl = new YamlStatementLoader();
        StmtsDocList erthing = ysl.load("testdocs/everything.yaml");
        List<StmtsDoc> stmtsDocs = erthing.getStmtsDocs();
        assertThat(stmtsDocs).hasSize(2);
        StmtsDoc stmtsDoc = stmtsDocs.get(0);
        List<StmtsBlock> blocks = stmtsDoc.getBlocks();
        assertThat(blocks).hasSize(1);
        StmtsBlock stmtsBlock = blocks.get(0);
        assertThat(stmtsBlock.getName()).isEqualTo("name");
    }


}