package io.engineblock.activities.stdout;

import activityconfig.StatementsLoader;
import activityconfig.yaml.StmtDef;
import activityconfig.yaml.StmtsDocList;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.RateLimiter;
import io.engineblock.activityapi.core.ActivityDefObserver;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.metrics.ExceptionMeterMetrics;
import io.engineblock.planning.OpSequence;
import io.engineblock.planning.SequencePlanner;
import io.engineblock.planning.SequencerType;
import io.engineblock.util.StrInterpolater;
import io.virtdata.core.AllDataMapperLibraries;
import io.virtdata.core.BindingsTemplate;
import io.virtdata.templates.StringBindings;
import io.virtdata.templates.StringBindingsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class StdoutActivity extends SimpleActivity implements ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger(StdoutActivity.class);
    private final Boolean showstmts;
    private final StmtsDocList stmtsDocList;
    public Timer bindTimer;
    public Timer executeTimer;
    public Timer resultTimer;
    public Histogram triesHisto;
    private List<StringBindingsTemplate> templates = new ArrayList<>();
    private Writer pw;
    private String fileName;
    private ExceptionMeterMetrics exceptionMeterMetrics;
    private int retry_delay = 0;
    private int retries;
    private RateLimiter rateLimiter;
    private OpSequence<StringBindings> opSequence;

    public StdoutActivity(ActivityDef activityDef) {
        super(activityDef);
        StrInterpolater interp = new StrInterpolater(activityDef);
        String yaml_loc = activityDef.getParams().getOptionalString("yaml").orElse("default");
        this.showstmts = activityDef.getParams().getOptionalBoolean("showstatements").orElse(false);
        this.fileName = activityDef.getParams().getOptionalString("filename").orElse("stdout");
        this.stmtsDocList = StatementsLoader.load(logger, yaml_loc, interp, "activities");
    }

    @Override
    public void shutdownActivity() {
        try {
            if (pw!=null) {
                pw.close();
            }
        } catch (Exception e) {
            logger.warn("error closing writer:" + e, e);
        }
    }

    @Override
    public void initActivity() {
        logger.debug("initializing activity: " + this.activityDef.getAlias());
        exceptionMeterMetrics = new ExceptionMeterMetrics(activityDef);

        onActivityDefUpdate(activityDef);

        opSequence = createTemplates();
        bindTimer = ActivityMetrics.timer(activityDef, "bind");
        executeTimer = ActivityMetrics.timer(activityDef, "execute");
        resultTimer = ActivityMetrics.timer(activityDef, "result");
        triesHisto = ActivityMetrics.histogram(activityDef, "tries");

        this.pw = createPrintWriter();

    }

    protected Writer createPrintWriter() {
        PrintWriter pw = null;
        if (fileName.toLowerCase().equals("stdout")) {
            pw = new PrintWriter(System.out);
        } else {
            try {
                pw = new PrintWriter(fileName);
                pw.print("");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException("Error initializing printwriter:" + e, e);
            }
        }
        return pw;
    }

    private OpSequence<StringBindings> createTemplates() {
        //List<StringBindingsTemplate> stringBindingsTemplates = new ArrayList<>();
        SequencerType sequencerType = SequencerType.valueOf(
                getParams().getOptionalString("seq").orElse("bucket")
        );
        SequencePlanner<StringBindings> sequencer = new SequencePlanner<>(sequencerType);

        String tagfilter = activityDef.getParams().getOptionalString("tags").orElse("");
        List<StmtDef> stmts = stmtsDocList.getStmts(tagfilter);
        if (stmts.size() > 0) {
            for (StmtDef stmt : stmts) {
                BindingsTemplate bt = new BindingsTemplate(AllDataMapperLibraries.get(), stmt.getBindings());
                StringBindingsTemplate sbt = new StringBindingsTemplate(stmt.getStmt(), bt);
                StringBindings sb = sbt.resolve();
                sequencer.addOp(sb,Long.valueOf(stmt.getParams().getOrDefault("ratio","1")));
            }
        } else if (stmtsDocList.getDocBindings().size() > 0) {
            logger.debug("Creating stdout statement template from bindings, since none is otherwise defined.");
            String generatedStmt = stmtsDocList.getDocBindings().keySet()
                    .stream().map(s -> "{" + s + "}")
                    .collect(Collectors.joining(",", "", "\n"));
            BindingsTemplate bt = new BindingsTemplate(AllDataMapperLibraries.get(), stmtsDocList.getDocBindings());
            StringBindingsTemplate sbt = new StringBindingsTemplate(generatedStmt, bt);
            StringBindings sb = sbt.resolve();
            sequencer.addOp(sb,1L);
        } else {
            logger.error("Unable to create a stdout statement if you have no active statements or bindings configured.");
        }

        OpSequence<StringBindings> opSequence = sequencer.resolve();
        if (getActivityDef().getCycleCount() == 0) {
            logger.debug("Adjusting cycle getChainSize for " + activityDef.getAlias() + " to " +
                    opSequence.getOps().size());
            getActivityDef().setCycles(String.valueOf(opSequence.getOps().size()));
        }

        return opSequence;
    }

    public List<StringBindingsTemplate> getTemplates() {
        return templates;
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        ParameterMap params = activityDef.getParams();
        this.retry_delay = params.getOptionalInteger("retry_delay").orElse(1000);
        this.retries = params.getOptionalInteger("retries").orElse(3);

        Optional<RateLimiter> newLimiter = activityDef.getParams().getOptionalDouble("targetrate")
                .map(RateLimiter::create);
        if (newLimiter.isPresent()) {
            RateLimiter newRateLimiter = newLimiter.get();
            if (rateLimiter==null || rateLimiter.getRate()!=newRateLimiter.getRate()) {
                rateLimiter = newRateLimiter;
                logger.debug("rate limiter adjusted to " + rateLimiter.getRate());
            }
        }
    }

    public synchronized void write(String statement) {
        int tries = 0;
        Exception e = null;
        if (rateLimiter!=null) {
            rateLimiter.acquire();
        }
        while (tries < retries) {
            tries++;
            if (pw == null) {
                pw = createPrintWriter();
            }
            try {
                pw.write(statement);
                pw.flush();
                return;
            } catch (Exception error) {
                logger.warn("Error during write:" + error, error);
                if (retry_delay > 0) {
                    try {
                        Thread.sleep(retry_delay);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        throw new RuntimeException("Retries exhausted: " + tries + "/" + retries);
    }

    public Boolean getShowstmts() {
        return showstmts;
    }
}