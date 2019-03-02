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
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.util.TagFilter;
import io.virtdata.api.ValuesBinder;
import io.virtdata.core.Bindings;
import io.virtdata.core.BindingsTemplate;
import io.virtdata.core.ContextualBindingsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpActivity extends SimpleActivity implements Activity, ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger(HttpActivity.class);
    private final ActivityDef activityDef;

    public ContextualBindingsTemplate getContextualBindingsTemplate() {
        return contextualBindingsTemplate;
    }

    private ContextualBindingsTemplate contextualBindingsTemplate;

    public StmtsDocList getStmtDocList() {
        return stmtDocList;
    }

    private final StmtsDocList stmtDocList;

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


    public HttpActivity(ActivityDef activityDef) {
        super(activityDef);

        this.activityDef = activityDef;

        String yaml_loc = activityDef.getParams().getOptionalString("yaml").orElse("default");

        stmtDocList = StatementsLoader.load(logger,yaml_loc, "activities");
    }



    @Override
    public void initActivity() {
        super.initActivity();

        stride = activityDef.getParams().getOptionalInteger("stride").orElse(1);

        maxTries = activityDef.getParams().getOptionalInteger("maxTries").orElse(1);
        showstmnts = activityDef.getParams().getOptionalBoolean("showstmnts").orElse(false);

        contextualBindingsTemplate = createContextualBindingsTemplate();

        bindTimer = ActivityMetrics.timer(activityDef, "bind");
        executeTimer = ActivityMetrics.timer(activityDef, "execute");
        resultTimer = ActivityMetrics.timer(activityDef, "result");
        triesHisto = ActivityMetrics.histogram(activityDef, "tries");
        rowCounter = ActivityMetrics.meter(activityDef, "rows");
        skippedTokens = ActivityMetrics.histogram(activityDef, "skipped-tokens");
        resultSuccessTimer = ActivityMetrics.timer(activityDef,"result-success");

        onActivityDefUpdate(activityDef);
    }
    @Override
    public synchronized void onActivityDefUpdate(ActivityDef activityDef) {
        super.onActivityDefUpdate(activityDef);
    }

    private ContextualBindingsTemplate createContextualBindingsTemplate() {

        String tagfilter = activityDef.getParams().getOptionalString("tags").orElse("");

        TagFilter ts = new TagFilter(tagfilter);
        List<StmtDef> stmts = stmtDocList.getStmtDocs().stream()
                .flatMap(d -> d.getStmts().stream())
                .filter(ts::matchesTagged)
                .collect(Collectors.toList());

        for (StmtDef stmt: stmts) {

            ParsedStmt parsed = stmt.getParsed();

            BindingsTemplate bindingsTemplate = new BindingsTemplate(parsed.getBindPoints());
            contextualBindingsTemplate = new ContextualBindingsTemplate<StmtDef, String>
                        (stmt, bindingsTemplate, new HttpStatementValueBinder(stride, this));
        }

        return contextualBindingsTemplate;
    }

    public Integer getMaxTries() {
        return maxTries;
    }

    public Boolean getShowstmts() {
        return showstmnts;
    }

    public static class HttpStatementValueBinder implements ValuesBinder<StmtDef, String> {

        private final static Pattern stmtToken = Pattern.compile("\\{(\\w+)\\}");
        private final HttpActivity httpActivity;
        private int repeat;

        public HttpStatementValueBinder(int repeat, HttpActivity httpActivity) {
            this.repeat = repeat;
            this.httpActivity = httpActivity;
        }

        @Override
        public synchronized String bindValues(StmtDef context, Bindings bindings, long cycle) {

            String docStatement = context.getStmt();

            String[] fields = getFields(docStatement, bindings.getAllMap(cycle));
            Map<String, Object> iteratedSuffixMap = bindings.getIteratedSuffixMap(cycle, repeat, fields);

            docStatement = getCookedStatement(docStatement);

            String statement = context.getStmt().trim();

            int i = 0;
            for (Map.Entry<String, Object> bindingPair : iteratedSuffixMap.entrySet()) {
                Pattern bindingToken = Pattern.compile("\\{(" + fields[i] + ")\\}");
                statement = bindingToken.matcher(statement).replaceAll(bindingPair.getValue().toString());
                i++;
            }

            return statement;
        }

        public String getCookedRepeatedStatement(String statement, int repeat) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < repeat; i++) {
                String varSuffix = String.valueOf(i);
                String indexedStmt = getCookedSuffixedStatement(statement,varSuffix);
                sb.append(indexedStmt);
                sb.append("\n");
            }
            return sb.toString();
        }

        public String getCookedSuffixedStatement(String statement, String suffix) {
            return stmtToken.matcher(statement).replaceAll("$1" + suffix);
        }

        public String getCookedStatement(String statement){
            return stmtToken.matcher(statement).replaceAll("$1");
        }

        public List<String> getCookedStatements(List<String> statements, int repeat){
            return statements.stream().map((String statement) -> {
                return getCookedRepeatedStatement(statement, repeat);
            }).collect(Collectors.toList());
        }

        public String[] getFields(String statement, Map<String, Object> bindings){
            ArrayList<String> fields = new ArrayList<String>();
            Matcher m = stmtToken.matcher(statement);
            while (m.find()) {
                String namedAnchor = m.group(1);
                if (!bindings.containsKey(namedAnchor)){
                    throw new RuntimeException("Named anchor " + namedAnchor + " not found in bindings!");
                }
                fields.add(m.group(1));
            }
            return fields.toArray(new String[0]);
        }

    }


}