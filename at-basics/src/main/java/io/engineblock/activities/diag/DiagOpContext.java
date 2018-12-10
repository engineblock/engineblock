package io.engineblock.activities.diag;

import io.engineblock.activityapi.core.ops.BaseOpContext;

import java.util.ArrayList;
import java.util.List;

public class DiagOpContext extends BaseOpContext {
    private String description;
    private List<String> diaglog = new ArrayList<>();

    DiagOpContext(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return super.toString() + ", description:'" + description;
    }
    public String getDescription() {
        return description;
    }
    public void log(String logline) {
        this.diaglog.add(logline);
    }
    public List<String> getDiagLog() {
        return diaglog;
    }
}
