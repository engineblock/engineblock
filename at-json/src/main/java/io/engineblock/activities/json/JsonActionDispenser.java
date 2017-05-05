package io.engineblock.activities.json;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActionDispenser;

public class JsonActionDispenser implements ActionDispenser {
    private JsonActivity activity;

    public JsonActionDispenser(JsonActivity activity){
        this.activity = activity;
    }

    @Override
    public Action getAction(int slot) {
        return new JsonAction(slot, activity);
    }
}
