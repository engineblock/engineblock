package io.engineblock.activityapi.core.ops.fluent;

public interface OpTracker<D> {
    void onStarted(StartedOp<D> op);
    void onCompleted(CompletedOp<D> op);

    void setMaxPendingOps(int maxPendingOps);
    int getMaxPendingOps();

    boolean isFull();
    int getPendingOps();

}
