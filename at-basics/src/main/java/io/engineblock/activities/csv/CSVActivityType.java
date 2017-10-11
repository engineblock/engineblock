package io.engineblock.activities.csv;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.core.ActionDispenser;
import io.engineblock.activityapi.core.ActivityType;
import io.engineblock.activityimpl.ActivityDef;

import java.util.Optional;

@AutoService(ActivityType.class)
public class CSVActivityType implements ActivityType<CSVActivity> {

    @Override
    public String getName() {
        return "csv";
    }

    @Override
    public CSVActivity getActivity(ActivityDef activityDef) {
        Optional<String> yaml = activityDef.getParams().getOptionalString("yaml");

        // sanity check that we have a yaml parameter, which contains our statements and bindings
        if (!yaml.isPresent()) {
            throw new RuntimeException("Currently, the csv activity type requires yaml activity parameter.");
        }

        // allow shortcut: yaml parameter provide the default alias name
        if (activityDef.getAlias().equals(ActivityDef.DEFAULT_ALIAS)) {
            activityDef.getParams().set("alias",yaml.get());
        }

        return new CSVActivity(activityDef);
    }

    @Override
    public ActionDispenser getActionDispenser(CSVActivity activity) {
        return new CSVActionDispenser(activity);
    }
}