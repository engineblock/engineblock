package io.engineblock.activitytypes.http;

import activityconfig.ParsedStmt;
import activityconfig.StatementsLoader;
import activityconfig.yaml.StmtDef;
import activityconfig.yaml.StmtsDocList;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.Activity;
import io.engineblock.activityapi.core.ActivityDefObserver;
import io.engineblock.activityapi.planning.OpSequence;
import io.engineblock.activityapi.planning.SequencePlanner;
import io.engineblock.activityapi.planning.SequencerType;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.metrics.ActivityMetrics;
import io.virtdata.core.BindingsTemplate;
import io.virtdata.templates.StringBindings;
import io.virtdata.templates.StringBindingsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class HttpActivity extends SimpleActivity implements Activity, ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger(HttpActivity.class);
    private final ActivityDef activityDef;

    public StmtsDocList getStmtsDocList() {
        return stmtsDocList;
    }

    private final StmtsDocList stmtsDocList;


    private int stride;
    private Integer maxTries;
    private Boolean showstmnts;
    public Timer bindTimer;
    public Timer executeTimer;
    public Histogram triesHisto;
    public Timer resultTimer;
    public Meter rowCounter;
    public Histogram skippedTokens;
    public Timer resultSuccessTimer;

    private String[] hosts;
    private int port;

    private OpSequence<StringBindings> opSequence;

    public HttpActivity(ActivityDef activityDef) {
        super(activityDef);
        this.activityDef = activityDef;
        String yaml_loc = activityDef.getParams()
                .getOptionalString("yaml")
                .orElse("default");

        stmtsDocList = StatementsLoader.load(logger,yaml_loc, "activities");
    }



    @Override
    public void initActivity() {
        super.initActivity();

        stride = activityDef.getParams().getOptionalInteger("stride").orElse(1);

        maxTries = activityDef.getParams().getOptionalInteger("maxTries").orElse(1);
        showstmnts = activityDef.getParams().getOptionalBoolean("showstmnts").orElse(false);

        hosts = activityDef.getParams().getOptionalString("host").orElse("localhost").split(",");
        port = activityDef.getParams().getOptionalInteger("port").orElse(80);


        opSequence = initOpSequencer();
        setDefaultsFromOpSequence(opSequence);

        bindTimer = ActivityMetrics.timer(activityDef, "bind");
        executeTimer = ActivityMetrics.timer(activityDef, "execute");
        resultTimer = ActivityMetrics.timer(activityDef, "result");
        triesHisto = ActivityMetrics.histogram(activityDef, "tries");
        rowCounter = ActivityMetrics.meter(activityDef, "rows");
        skippedTokens = ActivityMetrics.histogram(activityDef, "skipped-tokens");
        resultSuccessTimer = ActivityMetrics.timer(activityDef,"result-success");

        onActivityDefUpdate(activityDef);
    }

    private OpSequence<StringBindings> initOpSequencer() {
        SequencerType sequencerType = SequencerType.valueOf(
                getParams().getOptionalString("seq").orElse("bucket")
        );
        SequencePlanner<StringBindings> sequencer = new SequencePlanner<>(sequencerType);

        String tagfilter = activityDef.getParams().getOptionalString("tags").orElse("");
        List<StmtDef> stmts = stmtsDocList.getStmts(tagfilter);

        if (stmts.size() > 0) {
            for (StmtDef stmt : stmts) {
                ParsedStmt parsed = stmt.getParsed().orError();
                BindingsTemplate bt = new BindingsTemplate(parsed.getBindPoints());
                String statement = parsed.getPositionalStatement(Function.identity());
                Objects.requireNonNull(statement);

                StringBindingsTemplate sbt = new StringBindingsTemplate(stmt.getStmt(), bt);
                StringBindings sb = sbt.resolve();
                sequencer.addOp(sb,Long.valueOf(stmt.getParams().getOrDefault("ratio","1")));
            }
        } else {
            logger.error("Unable to create an HTTP statement if no bindings or statements are defined.");
        }
//
        OpSequence<StringBindings> opSequence = sequencer.resolve();
        return opSequence;
    }

    @Override
    public synchronized void onActivityDefUpdate(ActivityDef activityDef) {
        super.onActivityDefUpdate(activityDef);
    }

    public Integer getMaxTries() {
        return maxTries;
    }

    public Boolean getShowstmts() {
        return showstmnts;
    }

    public String[] getHosts() {
        return hosts;
    }

    public int getPort() {
        return port;
    }

    public OpSequence<StringBindings> getOpSequence() {
        return opSequence;
    }
}