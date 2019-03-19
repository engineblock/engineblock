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

package activityconfig.rawyaml;

import activityconfig.StatementsLoader;
import activityconfig.yaml.StmtDef;
import activityconfig.yaml.StmtsBlock;
import activityconfig.yaml.StmtsDoc;
import activityconfig.yaml.StmtsDocList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class StmtEscapingTest {

    private final static Logger logger = LoggerFactory.getLogger(StmtEscapingTest.class);
    private static List<StmtDef> defs;

    @BeforeClass
    public void testLayering() {

        StmtsDocList all = StatementsLoader.load(logger, "testdocs/escaped_stmts.yaml");
        assertThat(all).isNotNull();
        assertThat(all.getStmtDocs()).hasSize(1);
        StmtsDoc doc1 = all.getStmtDocs().get(0);

//        assertThat(doc1.getName()).isEqualTo("doc1");
        assertThat(doc1.getBlocks()).hasSize(1);

        StmtsBlock block1 = doc1.getBlocks().get(0);
        assertThat(block1.getBindings()).hasSize(0);
        assertThat(block1.getTags()).hasSize(0);
        assertThat(block1.getStmts()).hasSize(3);

        defs = block1.getStmts();
    }

    public void testBackslashEscape() {
        String s1 = defs.get(0).getStmt();
        assertThat(s1).isEqualTo("This is a \"statement\"");
    }

    public void testBackslashInBlock() {
        String s2 = defs.get(1).getStmt();
        assertThat(s2).isEqualTo("This is a \\\"statement\\\".\n");
    }

    public void testTripleQuotesInBlock() {
        String s3 = defs.get(2).getStmt();
        assertThat(s3).isEqualTo("This is a \"\"\"statement\"\"\".\n");
    }

}