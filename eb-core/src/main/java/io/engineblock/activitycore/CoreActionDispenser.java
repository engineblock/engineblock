package io.engineblock.activitycore;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActionDispenser;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreActionDispenser implements ActionDispenser {
    private final static Logger logger = LoggerFactory.getLogger(CoreActionDispenser.class);

    private ActivityDef activityDef;

    public CoreActionDispenser(ActivityDef activityDef) {
        this.activityDef = activityDef;
    }

    @Override
    public Action getAction(int slot) {
        return new CoreAction(activityDef, slot);
    }
}
