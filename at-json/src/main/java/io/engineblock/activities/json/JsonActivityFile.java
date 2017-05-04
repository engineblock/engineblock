package io.engineblock.activities.json;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.ActivityType;

@AutoService(ActivityType.class)
public class JsonActivityFile implements ActivityType {


    @Override
    public String getName() {
        return "json";
    }
}
