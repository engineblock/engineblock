package io.engineblock.activityapi.core.ops.fluent;

public interface OpTracker {
    TrackedOp allocate(long cycle);
    StartedOp start(TrackedOp op);
    CompletedOp stop(StartedOp op);
}
