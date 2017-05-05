package io.engineblock.activities.csv;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import io.engineblock.activities.csv.statements.FileStmtBlock;
import io.engineblock.activities.csv.statements.FileStmtDoc;
import io.engineblock.activities.csv.statements.FileStmtDocList;
import io.engineblock.activities.csv.statements.ReadyFileStatementTemplate;
import io.engineblock.activities.csv.statements.ReadyFileStatementsTemplate;
import io.engineblock.activities.csv.statements.YamlFileStatementLoader;
import io.engineblock.activityapi.ActivityDefObserver;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.metrics.ExceptionMeterMetrics;
import io.engineblock.util.StrInterpolater;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class FileActivity extends SimpleActivity implements ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger(FileActivity.class);
    private final FileStmtDocList stmtDocList;
    private final Boolean showstmts;
    private ReadyFileStatementsTemplate readyFileStatementsTemplate;

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

    public FileActivity(ActivityDef activityDef) {
        super(activityDef);
        StrInterpolater interp = new StrInterpolater(activityDef);
        String yaml_loc = activityDef.getParams().getOptionalString("yaml").orElse("default");
        this.showstmts = activityDef.getParams().getOptionalBoolean("showstatements").orElse(false);
        this.fileName = activityDef.getParams().getOptionalString("filename").orElse("out.txt");
        YamlFileStatementLoader yamlLoader = new YamlFileStatementLoader(interp);
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

        readyFileStatementsTemplate = createReadyFileStatementsTemplate();
        bindTimer = ActivityMetrics.timer(activityDef, "bind");
        executeTimer = ActivityMetrics.timer(activityDef, "execute");
        resultTimer = ActivityMetrics.timer(activityDef, "result");
        triesHisto = ActivityMetrics.histogram(activityDef, "tries");

        //clear out the file
        //empty out.txt on init
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(fileName);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private ReadyFileStatementsTemplate createReadyFileStatementsTemplate() {
        ReadyFileStatementsTemplate readyFileStatements = new ReadyFileStatementsTemplate();
        String tagfilter = activityDef.getParams().getOptionalString("tags").orElse("");
        List<FileStmtDoc> matchingStmtDocs = stmtDocList.getMatching(tagfilter);

        for (FileStmtDoc doc : matchingStmtDocs) {
            for (FileStmtBlock section : doc.getAllBlocks()) {
                Map<String, String> bindings = section.getBindings();
                int indexer = 0;
                for (String stmt : section.getStatements()) {
                    String name = section.getName() + "-" + indexer++;
                    ReadyFileStatementTemplate t = new ReadyFileStatementTemplate(name, stmt,bindings);
                    readyFileStatements.addTemplate(t);
                }
            }
        }

        if (getActivityDef().getCycleCount() == 0) {
            logger.debug("Adjusting cycle count for " + activityDef.getAlias() + " to " +
            readyFileStatements.size());
            getActivityDef().setCycles(String.valueOf(readyFileStatements.size()));
        }

        return readyFileStatements;
    }

    public ReadyFileStatementsTemplate getReadyFileStatements() {
        return readyFileStatementsTemplate;
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
