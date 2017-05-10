package io.engineblock.activities.csv;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import io.engineblock.activities.csv.statements.*;
import io.engineblock.activityapi.ActivityDefObserver;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.metrics.ExceptionMeterMetrics;
import io.engineblock.util.StrInterpolater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@SuppressWarnings("Duplicates")
public class CSVActivity extends SimpleActivity implements ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger(CSVActivity.class);
    private final CSVStmtDocList stmtDocList;
    private final Boolean showstmts;
    private ReadyCSVStatementsTemplate readyCSVStatementsTemplate;

    public Timer bindTimer;
    public Timer executeTimer;
    public Timer resultTimer;
    public Histogram triesHisto;
    private PrintWriter pw;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private String fileName;

    private ExceptionMeterMetrics exceptionMeterMetrics;

    public CSVActivity(ActivityDef activityDef) {
        super(activityDef);
        StrInterpolater interp = new StrInterpolater(activityDef);
        String yaml_loc = activityDef.getParams().getOptionalString("yaml").orElse("default");
        this.showstmts = activityDef.getParams().getOptionalBoolean("showstatements").orElse(false);
        this.fileName = activityDef.getParams().getOptionalString("filename").orElse("out.txt");
        YamlCSVStatementLoader yamlLoader = new YamlCSVStatementLoader(interp);
        stmtDocList = yamlLoader.load(yaml_loc, "activities");
    }

    @Override
    public void shutdownActivity() {
        pw.close();
    }

    @Override
    public void initActivity() {
        logger.debug("initializing activity: " + this.activityDef.getAlias());
        exceptionMeterMetrics = new ExceptionMeterMetrics(activityDef);

        onActivityDefUpdate(activityDef);

        readyCSVStatementsTemplate = createReadyCSVStatementsTemplate();
        bindTimer = ActivityMetrics.timer(activityDef, "bind");
        executeTimer = ActivityMetrics.timer(activityDef, "execute");
        resultTimer = ActivityMetrics.timer(activityDef, "result");
        triesHisto = ActivityMetrics.histogram(activityDef, "tries");

        //clear out the file
        //empty out.txt on init
        PrintWriter writer = null;
        try {
            this.pw  = new PrintWriter(fileName);
            pw.print("");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private ReadyCSVStatementsTemplate createReadyCSVStatementsTemplate() {
        ReadyCSVStatementsTemplate readyCSVStatements = new ReadyCSVStatementsTemplate();
        String tagfilter = activityDef.getParams().getOptionalString("tags").orElse("");
        List<CSVStmtDoc> matchingStmtDocs = stmtDocList.getMatching(tagfilter);

        for (CSVStmtDoc doc : matchingStmtDocs) {
            for (CSVStmtBlock section : doc.getAllBlocks()) {
                Map<String, String> bindings = section.getBindings();
                int indexer = 0;
                for (String stmt : section.getStatements()) {
                    String name = section.getName() + "-" + indexer++;
                    ReadyCSVStatementTemplate t = new ReadyCSVStatementTemplate(name, stmt,bindings);
                    readyCSVStatements.addTemplate(t);
                }
            }
        }

        if (getActivityDef().getCycleCount() == 0) {
            logger.debug("Adjusting cycle count for " + activityDef.getAlias() + " to " +
            readyCSVStatements.size());
            getActivityDef().setCycles(String.valueOf(readyCSVStatements.size()));
        }

        return readyCSVStatements;
    }

    public ReadyCSVStatementsTemplate getReadyFileStatements() {
        return readyCSVStatementsTemplate;
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        ParameterMap params = activityDef.getParams();
    }

    public ExceptionMeterMetrics getExceptionCountMetrics() {
        return exceptionMeterMetrics;
    }

    public synchronized void write(String statement) {
        pw.println(statement);
    }

    public Boolean getShowstmts() {
        return showstmts;
    }
}