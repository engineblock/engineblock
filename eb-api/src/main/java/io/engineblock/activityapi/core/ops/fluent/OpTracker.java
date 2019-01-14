package io.engineblock.activityapi.core.ops.fluent;

import io.engineblock.activityapi.core.ops.fluent.opfacets.OpEvents;
import io.engineblock.activityapi.core.ops.fluent.opfacets.TrackedOp;

import java.util.function.LongFunction;

public interface OpTracker<D> extends OpEvents<D> {

    void setMaxPendingOps(int maxPendingOps);
    int getMaxPendingOps();

    boolean isFull();
    int getPendingOps();

    void setCycleOpFunction(LongFunction<D> newOpFunction);
    TrackedOp<D> newOp(long cycle, OpEvents<D> strideTracker);

    boolean awaitCompletion(long timeout);
}
