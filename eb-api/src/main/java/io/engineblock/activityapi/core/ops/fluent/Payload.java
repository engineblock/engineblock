package io.engineblock.activityapi.core.ops.fluent;

public interface Payload<D> {
    D getData();
    void setData(D data);
}
