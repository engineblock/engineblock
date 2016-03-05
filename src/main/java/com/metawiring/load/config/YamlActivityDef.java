/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just a simple configuration object for yaml activity definitions.
 */
public class YamlActivityDef {

    private List<StatementDef> ddl = new ArrayList<StatementDef>();
    private List<StatementDef> dml = new ArrayList<StatementDef>();

    public void setDdl(List<StatementDef> ddl) {
        this.ddl = ddl;
    }
    public void setDml(List<StatementDef> dml) {
        this.dml = dml;
    }

    public List<StatementDef> getDml() {
        return dml;
    }

    public List<StatementDef> getDdl() {
        return ddl;
    }

}
