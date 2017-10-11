package io.engineblock.activities.csv;

import com.codahale.metrics.Timer;
import io.engineblock.activities.csv.errorhandling.ErrorResponse;
import io.engineblock.activities.csv.statements.ReadyCSVStatement;
import io.engineblock.activityapi.core.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("Duplicates")
public class CSVAction implements Action{

    private static final Logger logger = LoggerFactory.getLogger(CSVAction.class);

    private int slot;
    private CSVActivity activity;
    List<ReadyCSVStatement> readyCSVStatements;
    private int maxTries = 10;
    private boolean showstmts;
    private ErrorResponse retryableResponse;
    private ErrorResponse realErrorResponse;

    public CSVAction(int slot, CSVActivity activity) {
        this.slot = slot;
        this.activity = activity;
    }

    @Override
    public void init() {
        readyCSVStatements = activity.getReadyFileStatements().resolve();
    }

    @Override
    public int runCycle(long cycleValue) {
        ReadyCSVStatement readyCSVStatement;
        String statement = null;
        try (Timer.Context bindTime = activity.bindTimer.time()) {
            int selector = (int) (cycleValue % readyCSVStatements.size());
            readyCSVStatement = readyCSVStatements.get(selector);
            statement = readyCSVStatement.bind(cycleValue);
            showstmts = activity.getShowstmts();
            if (showstmts) {
                logger.info("FILE STATEMENT(cycle=" + cycleValue + "):\n" + statement);
            }
        }

        try {
            try (Timer.Context executeTime = activity.executeTimer.time()) {
                activity.write(statement);
            }
        } catch (Exception e) {
        }
        return 0;
    }

}