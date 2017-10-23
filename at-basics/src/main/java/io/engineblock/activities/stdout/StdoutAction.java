package io.engineblock.activities.stdout;

import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.Action;
import io.virtdata.templates.StringBindings;
import io.virtdata.templates.StringBindingsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class StdoutAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(StdoutAction.class);
    List<StringBindings> bindingsList;
    private int slot;
    private StdoutActivity activity;
    private int maxTries = 10;
    private boolean showstmts;

    public StdoutAction(int slot, StdoutActivity activity) {
        this.slot = slot;
        this.activity = activity;
    }

    @Override
    public void init() {
        bindingsList = activity.getTemplates()
                .stream().map(StringBindingsTemplate::resolve)
                .collect(Collectors.toList());
    }

    @Override
    public int runCycle(long cycleValue) {
        StringBindings stringBindings;
        String statement = null;
        try (Timer.Context bindTime = activity.bindTimer.time()) {
            int selector = (int) (cycleValue % bindingsList.size());
            stringBindings = bindingsList.get(selector);
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