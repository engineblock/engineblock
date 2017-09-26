package io.engineblock.activityapi.cycletracking;

/**
 * A cycle marker is simply a type that knows how to do something
 * useful with the result of a particular cycle.
 */
public interface Marker {

    /**
     * Mark the result of the numbered cycle with an integer value.
     * The meaning of the value provided is contextual to the way it is used.
     * (Each process will have its own status tables, etc.)
     *
     * @param completedCycle The cycle number being marked.
     * @param result the result ordinal
     */
    boolean onCycleResult(long completedCycle, int result);

}
