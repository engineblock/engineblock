package io.engineblock.activityapi.core.ops.fluent;

public class BaseOpTracker implements OpTracker {

    public TrackedOp allocate(long cycle) {
        return (TrackedOp) new OpImpl().setCycle(cycle);
    }

    @Override
    public StartedOp start(TrackedOp op) {
        return (StartedOp) op;
    }

    @Override
    public CompletedOp stop(StartedOp op) {
        return (CompletedOp) op;
    }
}
