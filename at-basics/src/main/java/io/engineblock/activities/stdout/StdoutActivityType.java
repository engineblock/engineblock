package io.engineblock.activities.stdout;

import com.google.auto.service.AutoService;
import io.engineblock.activityapi.core.Action;
import io.engineblock.activityapi.core.ActionDispenser;
import io.engineblock.activityapi.core.ActivityType;
import io.engineblock.activityimpl.ActivityDef;

import java.util.Optional;

@AutoService(ActivityType.class)
public class StdoutActivityType implements ActivityType<StdoutActivity> {

    @Override
    public String getName() {
        return "stdout";
    }

    @Override
    public StdoutActivity getActivity(ActivityDef activityDef) {
        Optional<String> yaml = activityDef.getParams().getOptionalString("yaml");

        // sanity check that we have a yaml parameter, which contains our statements and bindings
        if (!yaml.isPresent()) {
            throw new RuntimeException("Currently, the stdout activity type requires yaml activity parameter.");
        }

        // allow shortcut: yaml parameter provide the default alias name
        if (activityDef.getAlias().equals(ActivityDef.DEFAULT_ALIAS)) {
            activityDef.getParams().set("alias",yaml.get());
        }

        return new StdoutActivity(activityDef);
    }

    @Override
    public ActionDispenser getActionDispenser(StdoutActivity activity) {
        return new StdoutActionDispenser(activity);
    }

    private static class StdoutActionDispenser implements ActionDispenser {

        private final StdoutActivity activity;

        private StdoutActionDispenser(StdoutActivity activity) {
            this.activity = activity;
        }

        @Override
        public Action getAction(int slot) {
            return new StdoutAction(slot, activity);
        }
    }
}