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
        FileActivityType csvAt = new FileActivityType();
        String atname = csvAt.getName();
        assertThat(atname.equals("csv"));
        ActivityDef ad = ActivityDef.parseActivityDef("type=csv; yaml=csv-test;");
        FileActivity fa = csvAt.getActivity(ad);
        fa.initActivity();

        ActionDispenser actionDispenser = csvAt.getActionDispenser(fa);
        Action action = actionDispenser.getAction(1);
        action.init();

        //Had to comment this out for the test to pass, can't figure out 
        action.accept(1L);
    }
}