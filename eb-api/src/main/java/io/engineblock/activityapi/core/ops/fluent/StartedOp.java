package io.engineblock.activityapi.core.ops.fluent;

public interface StartedOp<D> extends Payload<D> {
    CompletedOp stop(int status);
}
