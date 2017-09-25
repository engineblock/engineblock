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

public class RawStmtDef extends BlockParams {

    private String statement;
    private String name;

    public RawStmtDef() {}

    public RawStmtDef(String name, String statement) {
        this.name = name;
        this.statement = statement;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getStatement() {
        return statement;
    }


    @Override
    public String toString() {
        return "StatementDef{" +
                "statement='" + statement + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RawStmtDef that = (RawStmtDef) o;

        if (statement != null ? !statement.equals(that.statement) : that.statement != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = statement != null ? statement.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
