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

import java.util.Map;

public class StmtDef {

    private StmtsBlock block;
    private String name;
    private String stmt;

    public StmtDef(StmtsBlock block, String name, String stmt) {
        this.block = block;
        this.name = name;
        this.stmt = stmt;
    }

    public String getName() {
        return name;
    }

    public String getStmt() {
        return stmt;
    }

    public Map<String,String> getBindings() {
        return block.getBindings();
    }

    public Map<String, String> getParams() {
        return block.getParams();
    }

    public Map<String,String> getTags() {
        return block.getTags();

    }
}
