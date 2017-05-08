package io.engineblock.activities.json;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActionDispenser;
import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.extractProperty;

public class JsonActivityTypeTest {

    @Test
    public void testJsonActivity(){
        File tempFile = null;
        try {
            tempFile = File.createTempFile("jsonActivity-", ".json");
            tempFile.deleteOnExit();
        }
        catch(IOException e) {
            e.printStackTrace();
            assert(false);
        }

        JsonActivityType jsonActivityType = new JsonActivityType();
        ActivityDef activityDef = ActivityDef.parseActivityDef(
                String.format("type=json; yaml=activities/json-test.yaml; filename=%s", tempFile.getAbsoluteFile())
        );

        JsonActivity jsonActivity = jsonActivityType.getActivity(activityDef);
        jsonActivity.initActivity();

        ActionDispenser actionDispenser = jsonActivityType.getActionDispenser(jsonActivity);
        Action action = actionDispenser.getAction(1);
        action.init();

        action.accept(1L);
        action.accept(2L);

        jsonActivity.shutdownActivity();

        String[] expectedOutput = {
                "{\"bar\":\"one\",\"foo\":\"one\",\"customer\":\"one\"}",
                "{\"bar\":\"two\",\"foo\":\"two\",\"customer\":\"two\"}"
        };

        try {
            int i = 0;

            BufferedReader br = new BufferedReader(new FileReader(tempFile.getAbsoluteFile()));
            String line;
            while ((line = br.readLine()) != null) {
                assertThat(line.equals(expectedOutput[i]));
                i++;
            }

            br.close();
        } catch(IOException|IndexOutOfBoundsException e){
            e.printStackTrace();
            assert(false);
        }

    }

}