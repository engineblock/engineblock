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

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;

import java.util.List;

public class ReadyStatements {
    private ReadyStatement[] readyStatements;

    public ReadyStatements(ReadyStatement... readyStatements) {
        this.readyStatements = readyStatements;
    }

    public ReadyStatements(List<ReadyStatement> readyStatements) {
        this(readyStatements.toArray(new ReadyStatement[readyStatements.size()]));
    }

    public ReadyStatement getNext(long moduloMultiple) {
        int selected = (int) (moduloMultiple % readyStatements.length);
        return readyStatements[selected];
    }

    public ReadyStatements setConsistencyLevel(ConsistencyLevel defaultConsistencyLevel) {
        for (ReadyStatement readyStatment: readyStatements) {
            PreparedStatement preparedStatement = readyStatment.getPreparedStatement();
            preparedStatement.setConsistencyLevel(defaultConsistencyLevel);
        }
        return this;
    }

    public ReadyStatement[] getReadyStatements() {
        return readyStatements;
    }

}
