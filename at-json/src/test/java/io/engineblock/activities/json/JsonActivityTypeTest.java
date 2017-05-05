package io.engineblock.activities.json;

import io.engineblock.activityimpl.ActivityDef;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonActivityTypeTest {


    @Test
    public void testJsonActivity(){
        JsonActivityType jsonActivityType = new JsonActivityType();
        ActivityDef activityDef = ActivityDef.parseActivityDef("type=json; yaml=activityDefTest.yaml");

        JsonActivity jsonActivity = jsonActivityType.getActivity(activityDef);
//        jsonActivity.initActivity();


//        try{
//
//            byte[] encoded = Files.readAllBytes(Paths.get("out.json"));
//            String producedOutput = new String(encoded, StandardCharsets.UTF_8);
//
//            String expectedOutput =
//                    "{\"bar\":\"zero\",\"foo\":\"zero\",\"customer\":\"zero\"}\n" +
//                    "{\"bar\":\"one\",\"foo\":\"one\",\"customer\":\"one\"}\n" +
//                    "{\"bar\":\"two\",\"foo\":\"two\",\"customer\":\"two\"}";
//
//            assertThat(producedOutput.equals(expectedOutput));
//        }
//        catch(IOException e)
//        {
//            e.printStackTrace();
//            assert(false);
//        }

    }

}