package io.engineblock.activities.diag;

import io.engineblock.activityapi.core.ops.BaseOpContext;

public class DiagOpContext extends BaseOpContext {
    private String description;

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
}
