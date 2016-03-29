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

package com.metawiring.load.core;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.metawiring.load.generator.GeneratorBindingList;
import com.metawiring.load.generator.GeneratorInstanceSource;

/**
 * A class that allows prepared statements and generatorBindings to be bundled together for easy use
 * within an activity. This is useful for activitytypes that need to juggle multiple types of
 * statements, and probably to simplify others.
 */
public class ReadyStatement {
    private final long startCycle;
    private final PreparedStatement preparedStatement;
    private final GeneratorBindingList generatorBindingList;

    public ReadyStatement(GeneratorInstanceSource generatorInstanceSource, PreparedStatement preparedStatement, long startCycle) {
        this.preparedStatement = preparedStatement;
        this.generatorBindingList = new GeneratorBindingList(generatorInstanceSource);
        this.startCycle = startCycle;
    }

    public BoundStatement bind() {
        Object[] all = generatorBindingList.getAll();
        BoundStatement bound = preparedStatement.bind(all);
        return bound;
    }

    public void addBinding(String varname, String generatorName) {
        generatorBindingList.bindGenerator(preparedStatement,varname,generatorName, startCycle);
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

}
