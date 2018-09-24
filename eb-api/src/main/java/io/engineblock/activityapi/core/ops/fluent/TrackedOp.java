package io.engineblock.activityapi.core.ops.fluent;

public interface TrackedOp<D> extends Payload<D> {
    StartedOp start();
    TrackedOp setWaitTime(long cycleDelay);
}
