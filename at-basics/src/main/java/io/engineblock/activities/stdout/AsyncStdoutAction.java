package io.engineblock.activities.stdout;

import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.BaseAsyncAction;
import io.engineblock.activityapi.core.ops.OpContext;
import io.engineblock.activityapi.core.ops.fluent.OpTracker;
import io.engineblock.activityapi.core.ops.fluent.TrackedOp;
import io.engineblock.activityapi.planning.OpSequence;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.motor.AsyncTracker;
import io.virtdata.templates.StringBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class AsyncStdoutAction extends BaseAsyncAction<StdoutOpContext,StdoutActivity> {
    private final static Logger logger = LoggerFactory.getLogger(AsyncStdoutAction.class);

    private OpSequence<StringBindings> sequencer;

    public AsyncStdoutAction(int slot, StdoutActivity activity) {
        super(activity,slot);
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        super.onActivityDefUpdate(activityDef);
        this.sequencer = activity.getOpSequence();
    }

    @Override
    protected AsyncTracker.Tracked startOpCycle(OpContext opc) {
        return null;
    }

    protected StdoutOpContext sstartOpCycle(StdoutOpContext opc) {

        opc.start();
        long cycle = opc.getCycle();
        try (Timer.Context bindTime = activity.bindTimer.time()) {
            opc.stringBindings = sequencer.get(cycle);
            opc.statement = opc.stringBindings.bind(cycle);
            if (activity.getShowstmts()) {
                logger.info("STMT(cycle=" + cycle + "):\n" + opc.statement);
            }
        }

        try (Timer.Context executeTime = activity.executeTimer.time()) {
            activity.write(opc.statement);
        } catch (Exception e) {
            throw new RuntimeException("Error writing output:" + e, e);
        }
        opc.stop(0);
        return opc;
    }

    @Override
    public boolean enqueue(TrackedOp opc) {
        return false;
    }

    @Override
    public OpTracker getTracker() {
        return null;
    }
}
