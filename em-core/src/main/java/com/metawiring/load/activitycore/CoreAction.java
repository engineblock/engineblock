package com.metawiring.load.activitycore;

import com.metawiring.load.activityapi.Action;
import com.metawiring.load.config.IActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreAction implements Action {
    private final static Logger logger = LoggerFactory.getLogger(CoreAction.class);

    private final int interval;
    private final IActivityDef activityDef;
    private final int slot;

    public CoreAction(IActivityDef activityDef, int slot) {
        this.activityDef = activityDef;
        this.slot = slot;
        this.interval = activityDef.getParams().getIntOrDefault("interval", 1000);
    }

    @Override
    public void accept(long value) {
        if ((value % interval) == 0) {
            logger.info(activityDef.getLogName() + "[" + slot + "]: cycle=" + value);
        }

    }
}
