package io.engineblock.activities.json;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.ActionDispenser;
import io.engineblock.activityapi.ActivityType;
import io.engineblock.activityimpl.ActivityDef;

@AutoService(ActivityType.class)
public class JsonActivityType implements ActivityType<JsonActivity> {


    @Override
    public String getName() {
        return "json";
    }

    @Override
    public ActionDispenser getActionDispenser(JsonActivity activity) {
        return new JsonActionDispenser(activity);
    }

    @Override
    public JsonActivity getActivity(ActivityDef activityDef) {
        return new JsonActivity(activityDef);
    }
}
