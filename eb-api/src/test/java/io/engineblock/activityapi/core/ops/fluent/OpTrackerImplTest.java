package io.engineblock.activityapi.core.ops.fluent;

import com.codahale.metrics.Counter;
import org.testng.annotations.Test;

@Test
public class OpTrackerImplTest {

    @Test
    public void testLifeCycle() {
        OpTrackerImpl<String> tracker = new OpTrackerImpl<>(new Counter());
        TrackedOp<String> tracked = new OpImpl<>(tracker);
        StartedOp<String> started = tracked.start();
        tracker.onStarted(started);
        CompletedOp stop = started.stop(23);
    }

}