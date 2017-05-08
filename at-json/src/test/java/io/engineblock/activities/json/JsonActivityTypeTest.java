package io.engineblock.activities.json;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActionDispenser;
import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonActivityTypeTest {

    @Test
    public void testJsonActivity(){
        JsonActivityType jsonActivityType = new JsonActivityType();
        ActivityDef activityDef = ActivityDef.parseActivityDef("type=json; yaml=json-test.yaml");

        JsonActivity jsonActivity = jsonActivityType.getActivity(activityDef);
        jsonActivity.initActivity();

        ActionDispenser actionDispenser = jsonActivityType.getActionDispenser(jsonActivity);
        Action action = actionDispenser.getAction(1);
        action.init();

        action.accept(1L);

    }

}