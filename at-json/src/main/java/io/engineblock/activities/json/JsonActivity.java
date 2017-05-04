package io.engineblock.activities.json;

import io.engineblock.activities.json.statements.*;
import io.engineblock.activityapi.Activity;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.util.StrInterpolater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


public class JsonActivity extends SimpleActivity implements Activity{
    private final static Logger logger = LoggerFactory.getLogger(JsonActivity.class);
    private final FileStmtDocList stmtDocList;
    private ReadyFileStatementsTemplate readyFileStatementsTemplate;


    public JsonActivity(ActivityDef activityDef) {
        super(activityDef);
        StrInterpolater interp = new StrInterpolater(activityDef);
        String yaml_loc = activityDef.getParams().getOptionalString("yaml").orElse("default");
        YamlFileStatementLoader yamlLoader = new YamlFileStatementLoader(interp);
        stmtDocList = yamlLoader.load(yaml_loc, "activities");
    }
    @Override
    public void initActivity(){
        logger.debug("initializing activity: " + this.activityDef.getAlias());
    }

    public ReadyFileStatementsTemplate getReadyFileStatements() {
        return readyFileStatementsTemplate;
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
}
