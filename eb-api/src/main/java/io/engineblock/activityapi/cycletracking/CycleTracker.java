package io.engineblock.activityapi;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>A cycle tracker allows concurrent threads to report on the
 * status of a cycle-oriented task, like an activity.</p>
 *
 * <p>Trackers are used to allow following activities to operate
 * only on cycles that were isCompleted by a different activity.</p>
 *
 * <p>When an activity is configured with a tracker, it must mark
 * completion on a specific cycle when it is complete, independent
 * of the order of completion.</p>
 *
 * <p>It is the job of the tracker to provide efficent concurrent access
 * to:
 * <UL>
 *     <LI>The lowest cycle number that has been isCompleted. This may return a lower
 *     cycle number that has been isCompleted within some bound, but it may not be higher..</LI>
 *     <LI>The highest cycle number that has been isCompleted. This may return a lower cycle
 *     number than the highest cycle number that has been isCompleted, but it must never be higher
 *     than the highest cycle numer that has actually been isCompleted.</LI>
 * </UL>
 * </p>
 */
public interface CycleTracker {

    /**
     * Mark a numbered cycle as isCompleted.
     * @param completedCycle The cycle to mark isCompleted
     */
    void markComplete(long completedCycle);

    /**
     * @param cycle the cycle number to check for completion
     * @return true if the numbered cycle has been marked isCompleted
     */
    boolean isCompleted(long cycle);

    /**
     * @return the minimum cycle isCompleted
     */
    long minCompleted();

    /**
     * @return the maximum cycle isCompleted
     */
    long maxCompleted();
}
