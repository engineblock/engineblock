package io.engineblock.activities.json;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.engineblock.activities.json.statements.ReadyFileStatement;
import io.engineblock.activityapi.Action;
import io.engineblock.activityapi.ActivityDefObserver;
import io.engineblock.activityimpl.ActivityDef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonAction implements Action {

    private final int slot;
    private final JsonActivity activity;
    List<ReadyFileStatement> readyFileStmts;


    public JsonAction(int slot, JsonActivity activity){
        this.slot = slot;
        this.activity = activity;
    }

    @Override
    public void init(){
        readyFileStmts = activity.getReadyFileStatements().resolve();
    }

    @Override
    public void accept(long cycle) {

        int selector = (int) (cycle % readyFileStmts.size());

        Map<String, Object> bindPoints = readyFileStmts.get(selector).bind(cycle);
        try (Timer.Context writeTime = activity.getJsonWriteTimer().time()) {
            activity.writeObject(bindPoints);
        }

    }

}
