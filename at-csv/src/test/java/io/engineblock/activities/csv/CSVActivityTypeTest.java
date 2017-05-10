package io.engineblock.activities.csv;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActionDispenser;
import io.engineblock.activityimpl.ActivityDef;
import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

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
