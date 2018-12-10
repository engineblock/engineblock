package io.engineblock.activityapi.core.ops.fluent;

public interface StartedOp<D> extends Payload<D> {
    CompletedOp<D> stop(int status);
}
