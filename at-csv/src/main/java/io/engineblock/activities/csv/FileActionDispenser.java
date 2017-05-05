package io.engineblock.activities.csv;

import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActionDispenser;

public class FileActionDispenser implements ActionDispenser {

    private FileActivity activity;

    public FileActionDispenser(FileActivity activity) {
        this.activity = activity;
    }

    @Override
    public Action getAction(int slot) {
        return new FileAction(slot, activity);
    }
}
