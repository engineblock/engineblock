package io.engineblock.activities.stdout;

import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.BaseAsyncAction;
import io.engineblock.activityapi.core.ops.fluent.CompletedOp;
import io.engineblock.activityapi.core.ops.fluent.StartedOp;
import io.engineblock.activityapi.core.ops.fluent.TrackedOp;
import io.engineblock.activityapi.planning.OpSequence;
import io.engineblock.activityimpl.ActivityDef;
import io.virtdata.templates.StringBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class AsyncStdoutAction extends BaseAsyncAction<StdoutOpContext, StdoutActivity> {
    private final static Logger logger = LoggerFactory.getLogger(AsyncStdoutAction.class);

    private OpSequence<StringBindings> sequencer;

    public AsyncStdoutAction(int slot, StdoutActivity activity) {
        super(activity, slot);
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        super.onActivityDefUpdate(activityDef);
        this.sequencer = activity.getOpSequence();
    }

    @Override
    public StdoutOpContext allocateOpData(long cycle) {

        StdoutOpContext opc = new StdoutOpContext();
        try (Timer.Context bindTime = activity.bindTimer.time()) {
            opc.stringBindings = sequencer.get(cycle);
            opc.statement = opc.stringBindings.bind(cycle);
            if (activity.getShowstmts()) {
                logger.info("STMT(cycle=" + cycle + "):\n" + opc.statement);
            }
        }
        return opc;
    }

    @Override
    public StartedOp<StdoutOpContext> startOpCycle(TrackedOp<StdoutOpContext> opc) {
        StartedOp<StdoutOpContext> started = opc.start();
        try (Timer.Context executeTime = activity.executeTimer.time()) {
            activity.write(opc.getData().statement);
        } catch (Exception e) {
            throw new RuntimeException("Error writing output:" + e, e);
        }
        return started;
    }

    @Override
    public CompletedOp<StdoutOpContext> completeOpCycle(StartedOp<StdoutOpContext> opc) {
        return opc.stop(0);
    }

}
