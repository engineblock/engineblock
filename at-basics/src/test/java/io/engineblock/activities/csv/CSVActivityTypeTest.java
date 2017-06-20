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

package io.engineblock.activities.csv;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActionDispenser;
import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by sebastianestevez on 5/5/17.
 */
public class CSVActivityTypeTest {
    @Test
    public void testDiagActivity() {
        CSVActivityType csvAt = new CSVActivityType();
        String atname = csvAt.getName();
        assertThat(atname.equals("csv"));
        ActivityDef ad = ActivityDef.parseActivityDef("type=csv; yaml=csv-test;");
        CSVActivity csvActivity = csvAt.getActivity(ad);
        ActionDispenser actionDispenser = csvAt.getActionDispenser(csvActivity);
        Action action = actionDispenser.getAction(1);
        //csvActivity.initActivity();
        //action.accept(1L);
    }
}
