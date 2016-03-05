/*
*   Copyright 2015 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package com.metawiring.load.activitytypes.cql;

import com.google.auto.service.AutoService;
import com.metawiring.load.activityapi.ActionDispenser;
import com.metawiring.load.activityapi.ActivityType;
import com.metawiring.load.config.ActivityDef;

@AutoService(ActivityType.class)
public class CQLActivityType implements ActivityType {

    @Override
    public String getName() {
        return "cql";
    }

    @Override
    public <T extends ActivityDef> ActionDispenser getActionDispenser(T activityDef) {
        return new CQLActionDispenser(activityDef);
    }


}
