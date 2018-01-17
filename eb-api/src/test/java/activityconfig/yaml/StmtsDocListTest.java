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

import activityconfig.StatementsLoader;
import org.assertj.core.data.MapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class StmtsDocListTest {
    private static final Logger logger = LoggerFactory.getLogger(StmtsDocListTest.class);

    private StmtsDocList doclist;

    private LinkedHashMap<String, String> doc0bindings = new LinkedHashMap<String, String>() {{
        put("b2", "b2d");
        put("b1", "b1d");
    }};

    private LinkedHashMap<String, String> doc0params = new LinkedHashMap<String,String>() {{
        put("param1","value1");
    }};

    private LinkedHashMap<String, String> doc0tags = new LinkedHashMap<String,String>() {{
        put("atagname","atagvalue");
    }};


    @BeforeClass
    public void testLoadYaml() {
        doclist = StatementsLoader.load(logger, "testdocs/everything.yaml");
    }

    @Test
    public void testStatementNameOverride() {
        StmtsDocList doclist2 = StatementsLoader.load(logger, "testdocs/statement_names.yaml");
        assertThat(doclist2).isNotNull();
        assertThat(doclist2.getStmtDocs()).hasSize(1);
        StmtsDoc doc1 = doclist2.getStmtDocs().get(0);
        assertThat(doc1.getBlocks()).hasSize(1);
        StmtsBlock doc1block1 = doc1.getBlocks().get(0);
        assertThat(doc1block1.getStmts()).hasSize(6);
        List<StmtDef> stmts = doc1block1.getStmts();
        assertThat(stmts.get(0).getName()).isEqualTo("thefirst");
        assertThat(stmts.get(0).getStmt()).isEqualTo("the_first_statement_body");
        assertThat(stmts.get(1).getName()).isEqualTo("block0--2");
        assertThat(stmts.get(1).getStmt()).isEqualTo("the_second_statement_body");
        assertThat(stmts.get(2).getName()).isEqualTo("block0--thethird");
        assertThat(stmts.get(2).getStmt()).isEqualTo("the third statement body");
        assertThat(stmts.get(3).getName()).isEqualTo("thefourth");
        assertThat(stmts.get(3).getStmt()).isEqualTo("the fourth statement body");
        assertThat(stmts.get(4).getName()).isEqualTo("thefifth");
        assertThat(stmts.get(4).getStmt()).isEqualTo("This is the fifth statement body.");
        assertThat(stmts.get(5).getName()).isEqualTo("block0--6");
        assertThat(stmts.get(5).getStmt()).isEqualTo("This does> not count");
    }



    @Test
    public void testBlocksInheritDocData() {
        assertThat(doclist).isNotNull();
        assertThat(doclist.getStmtDocs()).hasSize(2);
        StmtsDoc doc1 = doclist.getStmtDocs().get(0);

        assertThat(doc1.getBlocks()).hasSize(1);
        StmtsBlock doc1block0 = doc1.getBlocks().get(0);

        assertThat(doc1.getTags()).isEqualTo(doc0tags);
        assertThat(doc1.getBindings()).isEqualTo(doc0bindings);
        assertThat(doc1.getParams()).isEqualTo(doc0params);

        assertThat(doc1block0.getName()).isEqualTo("doc1--block0");
        assertThat(doc1block0.getBindings()).containsExactly(MapEntry.entry("b2","b2d"),MapEntry.entry("b1","b1d"));
        assertThat(doc1block0.getParams()).containsExactly(MapEntry.entry("param1","value1"));
        assertThat(doc1block0.getTags()).containsExactly(MapEntry.entry("atagname","atagvalue"));

    }

    @Test
    public void testStmtInheritsBlockData() {
        StmtsDoc doc0 = doclist.getStmtDocs().get(0);
        List<StmtDef> stmts1 = doc0.getBlocks().get(0).getStmts();
        assertThat(stmts1).hasSize(2);

        StmtsBlock block0 = doc0.getBlocks().get(0);
        assertThat(block0.getBindings()).containsExactly(MapEntry.entry("b2","b2d"),MapEntry.entry("b1","b1d"));
        assertThat(block0.getParams()).containsExactly(MapEntry.entry("param1","value1"));
        assertThat(block0.getTags()).containsExactly(MapEntry.entry("atagname","atagvalue"));

        assertThat(stmts1.get(0).getBindings()).containsExactly(MapEntry.entry("b2","b2d"),MapEntry.entry("b1","b1d"));
        assertThat(stmts1.get(0).getParams()).containsExactly(MapEntry.entry("param1","value1"));
        assertThat(stmts1.get(0).getTags()).containsExactly(MapEntry.entry("atagname","atagvalue"));

        assertThat(stmts1.get(1).getBindings()).containsExactly(MapEntry.entry("b2","b2d"),MapEntry.entry("b1","b1d"));
        assertThat(stmts1.get(1).getParams()).containsExactly(MapEntry.entry("param1","value1"));
        assertThat(stmts1.get(1).getTags()).containsExactly(MapEntry.entry("atagname","atagvalue"));

    }

    @Test
    public void testBlockLayersDocData() {
        StmtsDoc doc1 = doclist.getStmtDocs().get(1);
        StmtsBlock block0 = doc1.getBlocks().get(0);

        Map<String, String> doc1block0tags = block0.getTags();
        Map<String, String> doc1block0params = block0.getParams();
        Map<String, String> doc1block0bindings = block0.getBindings();

        assertThat(doc1block0tags).hasSize(3);
        assertThat(doc1block0tags).containsOnly(
                MapEntry.entry("root1","val1"),
                MapEntry.entry("root2","val2"),
                MapEntry.entry("block1tag","tag-value1"));
        assertThat(doc1block0params).containsOnly(
                MapEntry.entry("foobar","baz"),
                MapEntry.entry("timeout","23423"));
        assertThat(doc1block0bindings).containsOnly(
                MapEntry.entry("b12","b12d"),
                MapEntry.entry("b11","override-b11"),
                MapEntry.entry("foobar","overized-beez"));
    }

    @Test
    public void testStmtsGetter() {
        StmtsDoc doc1 = doclist.getStmtDocs().get(1);
        List<StmtDef> stmts = doc1.getStmts();
        assertThat(stmts).hasSize(4);
    }

    @Test
    public void testFilteredStmts() {
        List<StmtDef> stmts = doclist.getStmts("");
        assertThat(stmts).hasSize(6);
        stmts = doclist.getStmts("root1:value23");
        assertThat(stmts).hasSize(2);
    }

}