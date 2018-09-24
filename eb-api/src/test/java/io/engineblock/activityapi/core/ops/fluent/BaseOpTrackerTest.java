package io.engineblock.activityapi.core.ops.fluent;

import org.testng.annotations.Test;

@Test
public class BaseOpTrackerTest {

    @Test
    public void testLifeCycle() {
        BaseOpTracker tracker = new BaseOpTracker();
        TrackedOp tracked = tracker.allocate(0L);
        StartedOp started = tracker.start(tracked);
        CompletedOp stop = started.stop(23);
    }

}