package io.engineblock.activityapi.cycletracking;

/**
 * A cycle marker is simply a type that knows how to do something
 * useful with the result of a particular cycle.
 */
public interface CycleResultSink {

    /**
     * Mark a numbered cycle with a specific result.
     * @param completedCycle The cycle to mark isCycleCompleted
     */
//    boolean markResult(long completedCycle, byte result);

    boolean markResult(long completedCycle, int result);
//    {
//        return markResult(completedCycle, result & (byte) 127);
//    }


}
