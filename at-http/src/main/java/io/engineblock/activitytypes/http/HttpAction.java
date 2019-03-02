package io.engineblock.activitytypes.http;

import activityconfig.yaml.StmtDef;
import activityconfig.yaml.StmtsBlock;
import activityconfig.yaml.StmtsDoc;
import activityconfig.yaml.StmtsDocList;
import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.ActivityDefObserver;
import io.engineblock.activityapi.core.MultiPhaseAction;
import io.engineblock.activityapi.core.SyncAction;
import io.engineblock.activityimpl.ActivityDef;
import io.virtdata.core.ContextualBindingsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class HttpAction implements SyncAction, MultiPhaseAction, ActivityDefObserver {
    private final static Logger logger = LoggerFactory.getLogger(HttpAction.class);

    private final ActivityDef activityDef;
    private final HttpActivity httpActivity;
    private final int slot;

    // dynamic parameters
    private int maxTries = 1;    // how many cycles a statement will be attempted for before giving up


    private List<StmtDef> statements = new ArrayList<StmtDef>();
    private boolean showstmnts;
    private long nanoStartTime;
    private ContextualBindingsTemplate contextualBindingsTemplate;
    private StmtsDocList stmtDocList;


    public HttpAction(ActivityDef activityDef, int slot, HttpActivity httpActivity) {
        this.activityDef = activityDef;
        this.httpActivity = httpActivity;
        this.slot = slot;
        onActivityDefUpdate(activityDef);
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        this.maxTries = httpActivity.getMaxTries();
        this.showstmnts = httpActivity.getShowstmts();
    }

    @Override
    public boolean incomplete() {
        return false;
    }

    @Override
    public int runPhase(long value) {
        return runCycle(value);
    }

    @Override
    public void init() {
        onActivityDefUpdate(activityDef);

        this.contextualBindingsTemplate = httpActivity.getContextualBindingsTemplate();
        this.stmtDocList = httpActivity.getStmtDocList();
        for (StmtsDoc doc : stmtDocList.getStmtDocs() ){
            for (StmtsBlock stmtsBlock : doc.getBlocks()) {
                List<StmtDef> stmtsFromBlock = stmtsBlock.getStmts();
                statements.addAll(stmtsFromBlock);
            }
        }
    }

    @Override
    public int runCycle(long cycleValue) {

        String requestType = null;
        String boundStatement = null;
        InputStream result = null;

        final Map<String, String> queryParamMap = new HashMap<String, String>();

        int tries = 0;

        try (Timer.Context bindTime = httpActivity.bindTimer.time()) {
            if (statements.size() == 0){
                logger.warn("No statements found.");
            }
            Iterator<StmtDef> it = statements.iterator();
            while (it.hasNext()) {
                StmtDef stmt = it.next();
                requestType = stmt.getParams().getOrDefault("requestType", "GET");
                String statement = (String) contextualBindingsTemplate.resolveBindings().bind(cycleValue);

                // Validate that the statement contains arguments to parse
                if (!statement.equals("/")) {
                    for(String queryParam : statement.split("&")){
                        String[] splitParam = queryParam.split("=");
                        queryParamMap.put(splitParam[0], splitParam[1]);
                    }
                }

                String[] hosts = activityDef.getParams().getOptionalString("host").orElse("localhost").split(",");
                int port = activityDef.getParams().getOptionalInteger("port").orElse(80);

                String host = hosts[(int) cycleValue % hosts.length];


                String[] splitStatement = statement.split("\\?");
                String path, query;
                if (splitStatement.length >= 2) {
                    path = splitStatement[0];
                    query = splitStatement[1];
                    URI uri = new URI(
                            "http",
                            null,
                            host,
                            port,
                            path,
                            query,
                            null);

                    boundStatement = uri.toString();
                }

                if (this.showstmnts) {
                    logger.info("Bound STATEMENT(cycle=" + cycleValue + "):\n" + boundStatement);
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        nanoStartTime=System.nanoTime();

        while (tries < maxTries) {
            tries++;

            try (Timer.Context executeTime = httpActivity.executeTimer.time()) {
                try {

                    URL url = new URL(boundStatement);


                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(requestType);
                    result = conn.getInputStream();


                } catch(IOException e) {
                    e.printStackTrace();
                }
            }

            Timer.Context resultTime = httpActivity.resultTimer.time();
            try {
                StringBuilder res = new StringBuilder();

                BufferedReader rd = new BufferedReader(new InputStreamReader(result));
                String line;
                while ((line = rd.readLine()) != null) {
                    res.append(line);
                }
                rd.close();

            } catch (Exception e) {
                long resultNanos = resultTime.stop();
                resultTime=null;
            } finally {
                if (resultTime!=null) {
                    resultTime.stop();
                }

            }
        }
        long resultNanos=System.nanoTime() - nanoStartTime;
        httpActivity.resultSuccessTimer.update(resultNanos, TimeUnit.NANOSECONDS);


        return 0;
    }

    protected HttpActivity getHttpActivity() {
        return httpActivity;
    }
}