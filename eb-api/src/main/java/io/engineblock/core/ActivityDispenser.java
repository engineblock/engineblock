package io.engineblock.core;

import io.engineblock.activityapi.Activity;
import io.engineblock.activityimpl.ActivityDef;

public interface ActivityDispenser {
    Activity getActivity(ActivityDef activityDef);
}
