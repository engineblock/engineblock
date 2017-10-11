package io.engineblock.activities.csv;

import io.engineblock.activityapi.core.Action;
import io.engineblock.activityapi.core.ActionDispenser;

public class CSVActionDispenser implements ActionDispenser {

    private CSVActivity activity;

    public CSVActionDispenser(CSVActivity activity) {
        this.activity = activity;
    }

    @Override
    public Action getAction(int slot) {
        return new CSVAction(slot, activity);
    }
}
