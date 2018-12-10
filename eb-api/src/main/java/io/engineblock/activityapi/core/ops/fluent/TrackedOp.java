package io.engineblock.activityapi.core.ops.fluent;

public interface TrackedOp<D> extends Payload<D> {
    StartedOp<D> start();
    TrackedOp<D> setWaitTime(long cycleDelay);
}
