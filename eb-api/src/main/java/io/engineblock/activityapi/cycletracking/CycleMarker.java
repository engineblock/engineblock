package io.engineblock.activityapi.cycletracking;

/**
 * A cycle marker is simply a type that knows how to do something
 * useful with the result of a particular cycle.
 */
public interface CycleMarker {

    /**
     * Mark a numbered cycle with a specific result.
     * @param completedCycle The cycle to mark isCompleted
     */
    void markResult(long completedCycle, int result);

}
