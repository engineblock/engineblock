package io.engineblock.activities.csv;

import com.codahale.metrics.Timer;
import io.engineblock.activities.csv.errorhandling.ErrorResponse;
import io.engineblock.activities.csv.statements.ReadyFileStatement;
import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActivityDefObserver;
import io.engineblock.activityimpl.ActivityDef;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.virtdata.api.*;

@SuppressWarnings("Duplicates")
public class FileAction implements Action, ActivityDefObserver {

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

        //empty out.txt on init
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("out.txt");
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        onActivityDefUpdate(activity.getActivityDef());
        readyFileStmts = activity.getReadyFileStatements().resolve();
    }

    @Override
    public void accept(long cycleValue) {


        ReadyFileStatement readyFileStringStatement;
        //is this super inefficient by re-openning the file each time? Does it matter?
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter("out.txt", true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int tries = 0;

        String statement = null;
        try (Timer.Context bindTime = activity.bindTimer.time()) {
            int selector = (int) (cycleValue % readyFileStmts.size());
            readyFileStringStatement = readyFileStmts.get(selector);
            statement = readyFileStringStatement.bind(cycleValue);
            if (showstmts) {
                logger.info("FILE STATEMENT(cycle=" + cycleValue + "):\n" + statement);
            }
        }


        try {
            try (Timer.Context executeTime = activity.executeTimer.time()) {
                pw.println(statement);
            }
        } catch (Exception e) {
        } finally {
            activity.triesHisto.update(tries);
        }
        pw.close();
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {

        this.maxTries = activityDef.getParams().getOptionalInteger("maxtries").orElse(10);
        this.showstmts = activityDef.getParams().getOptionalBoolean("showcql").orElse(false);

        boolean diagnose = activityDef.getParams().getOptionalBoolean("diagnose").orElse(false);

        if (diagnose) {
            logger.warn("You are wiring all error handlers to stop for any exception." +
                    " This is useful for setup and troubleshooting, but unlikely to" +
                    " be useful for long-term or bulk testing, as retryable errors" +
                    " are normal in a busy system.");
            this.realErrorResponse = this.retryableResponse = ErrorResponse.stop;
        } else {
            String realErrorsSpec = activityDef.getParams()
                    .getOptionalString("realerrors").orElse(ErrorResponse.stop.toString());
            this.realErrorResponse = ErrorResponse.valueOf(realErrorsSpec);

            String retryableSpec = activityDef.getParams()
                    .getOptionalString("retryable").orElse(ErrorResponse.retry.toString());

            this.retryableResponse = ErrorResponse.valueOf(retryableSpec);
        }
    }
}