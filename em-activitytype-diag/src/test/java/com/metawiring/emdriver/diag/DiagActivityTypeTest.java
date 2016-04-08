package com.metawiring.emdriver.diag;

import com.metawiring.load.activityapi.ActionDispenser;
import com.metawiring.load.activityapi.ActivityDef;
import com.metawiring.load.activityapi.Action;
import com.metawiring.load.config.ActivityDefImpl;
import org.testng.annotations.Test;

/*
*   Copyright 2016 jshook
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
public class DiagActivityTypeTest {

    @Test
    public void testDiagActivity() {
        DiagActivityType da = new DiagActivityType();
        da.getName();
        ActivityDef ad = ActivityDefImpl.parseActivityDef("type=diag;");
        ActionDispenser actionDispenser = da.getActionDispenser(ad);
        Action action = actionDispenser.getAction(1);
        action.accept(1L);
    }

}