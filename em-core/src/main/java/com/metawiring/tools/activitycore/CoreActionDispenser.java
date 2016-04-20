package com.metawiring.tools.activitycore;

import com.metawiring.tools.activityapi.Action;
import com.metawiring.tools.activityapi.ActionDispenser;
import com.metawiring.tools.activityapi.ActivityDef;
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
