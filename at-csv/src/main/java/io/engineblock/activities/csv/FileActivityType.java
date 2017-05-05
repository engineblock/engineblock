package io.engineblock.activities.csv;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.ActionDispenser;
import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.ActivityType;
import io.engineblock.activityimpl.ActivityDef;

import java.util.Optional;

@AutoService(ActivityType.class)
public class FileActivityType implements ActivityType<FileActivity> {

    @Override
    public String getName() {
        return "csv";
    }

    @Override
    public FileActivity getActivity(ActivityDef activityDef) {
        Optional<String> yaml = activityDef.getParams().getOptionalString("yaml");

        // sanity check that we have a yaml parameter, which contains our statements and bindings
        if (!yaml.isPresent()) {
            throw new RuntimeException("Currently, the file activity type requires yaml activity parameter.");
        }

        // allow shortcut: yaml parameter provide the default alias name
        if (activityDef.getAlias().equals(ActivityDef.DEFAULT_ALIAS)) {
            activityDef.getParams().set("alias",yaml.get());
        }

        return new FileActivity(activityDef);
    }

    @Override
    public ActionDispenser getActionDispenser(FileActivity activity) {
        return new FileActionDispenser(activity);
    }
}