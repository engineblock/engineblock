package io.engineblock.activityapi.core.ops.fluent;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.ops.fluent.opfacets.CompletedOp;
import io.engineblock.activityapi.core.ops.fluent.opfacets.EventedOpImpl;
import io.engineblock.activityapi.core.ops.fluent.opfacets.StartedOp;
import io.engineblock.activityapi.core.ops.fluent.opfacets.TrackedOp;
import org.testng.annotations.Test;

@Test
public class OpTrackerImplTest {

    @Test
    public void testLifeCycle() {
        OpTrackerImpl<String> tracker = new OpTrackerImpl<String>("test", 0, new Timer(), new Timer(), new Counter());
        TrackedOp<String> tracked = new EventedOpImpl<>(tracker);
        StartedOp<String> started = tracked.start();
        tracker.onOpStarted(started);
        CompletedOp stop = started.stop(23);
    }

}