package com.metawiring.load.activityapi;

public enum SlotState {

    // Initial state after creation of this control
    Initialized("I>"),
    // This thread is running. This should only be set by the controlled thread
    Started("S>"),
    // This thread has completed all of its activity, and will do no further work without new input
    Finished("F."),
    // The thread has been requested to stop. This says nothing of the internal state.
    Stopping("-\\"),
    // The thread has stopped. This should only be set by the controlled thread
    Stopped("_.");

    private String runcode;

    SlotState(String runcode) {
        this.runcode = runcode;
    }

    public String getCode() { return this.runcode; }

}
