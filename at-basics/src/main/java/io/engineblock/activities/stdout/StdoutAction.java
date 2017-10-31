package io.engineblock.activities.stdout;

import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.Action;
import io.engineblock.planning.OpSequence;
import io.virtdata.templates.StringBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class StdoutAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(StdoutAction.class);
    private int slot;
    private StdoutActivity activity;
    private int maxTries = 10;
    private boolean showstmts;
    private OpSequence<StringBindings> sequencer;

    public StdoutAction(int slot, StdoutActivity activity) {
        this.slot = slot;
        this.activity = activity;
    }

    @Override
    public void init() {
        this.sequencer = activity.getOpSequence();
    }

    @Override
    public int runCycle(long cycleValue) {
        StringBindings stringBindings;
        String statement = null;
        try (Timer.Context bindTime = activity.bindTimer.time()) {
            stringBindings = sequencer.get(cycleValue);
            statement = stringBindings.bind(cycleValue);
            showstmts = activity.getShowstmts();
            if (showstmts) {
                logger.info("STMT(cycle=" + cycleValue + "):\n" + statement);
            }
        }

        try (Timer.Context executeTime = activity.executeTimer.time()) {
            activity.write(statement);
        } catch (Exception e) {
            throw new RuntimeException("Error writing output:" + e, e);
        }
        return 0;
    }

}