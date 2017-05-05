package io.engineblock.activities.csv;

import com.codahale.metrics.Timer;
import io.engineblock.activities.csv.errorhandling.ErrorResponse;
import io.engineblock.activities.csv.statements.ReadyFileStatement;
import io.engineblock.activityapi.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("Duplicates")
public class FileAction implements Action{

    private static final Logger logger = LoggerFactory.getLogger(FileAction.class);

    private int slot;
    private FileActivity activity;
    List<ReadyFileStatement> readyFileStmts;
    private int maxTries = 10;
    private boolean showstmts;
    private ErrorResponse retryableResponse;
    private ErrorResponse realErrorResponse;

    public FileAction(int slot, FileActivity activity) {
        this.slot = slot;
        this.activity = activity;
    }

    @Override
    public void init() {
        readyFileStmts = activity.getReadyFileStatements().resolve();
    }

    @Override
    public void accept(long cycleValue) {


        ReadyFileStatement readyFileStringStatement;

        String statement = null;
        try (Timer.Context bindTime = activity.bindTimer.time()) {
            int selector = (int) (cycleValue % readyFileStmts.size());
            readyFileStringStatement = readyFileStmts.get(selector);
            statement = readyFileStringStatement.bind(cycleValue);
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
    }

}