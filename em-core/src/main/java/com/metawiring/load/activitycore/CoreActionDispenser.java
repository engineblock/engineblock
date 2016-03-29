package com.metawiring.load.activitycore;

import com.metawiring.load.activityapi.Action;
import com.metawiring.load.activityapi.ActionDispenser;
import com.metawiring.load.activityapi.ActivityDef;
import com.metawiring.load.config.ActivityDefImpl;
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
