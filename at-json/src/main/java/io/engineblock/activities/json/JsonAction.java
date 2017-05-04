package io.engineblock.activities.json;

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
import java.util.List;

public class JsonAction implements Action, ActivityDefObserver {

    private final int slot;
    private final JsonActivity activity;
    List<ReadyFileStatement> readyFileStmts;


    public JsonAction(int slot, JsonActivity activity){
        this.slot = slot;
        this.activity = activity;
    }



    @Override
    public void init(){

//        try {
//            JsonFactory factory = new JsonFactory();
//            JsonGenerator generator = factory.createGenerator(new FileOutputStream("out.json"), JsonEncoding.UTF8);
//
//        } catch(IOException e) {
//            e.printStackTrace();
//        }
//
        List<ReadyFileStatement> readyFileStatements = activity.getReadyFileStatements().resolve();

    }

    @Override
    public void accept(long cycle) {
//        JsonFactory factory = new JsonFactory();
//        JsonGenerator generator = null;
//        try{
//            generator= factory.createGenerator(new FileOutputStream("out.json"), JsonEncoding.UTF8);
//
//            generator.writeStartObject();
//
//        }
//        catch(IOException e) {
//            e.printStackTrace();
//        }

//        System.out.println("INFO: " + readyFileStmts.get(0).toString());

    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {

//        this.maxTries = activityDef.getParams().getOptionalInteger("maxtries").orElse(10);
//        this.showstmts = activityDef.getParams().getOptionalBoolean("showcql").orElse(false);
//
//        boolean diagnose = activityDef.getParams().getOptionalBoolean("diagnose").orElse(false);
//
//        if (diagnose) {
//            logger.warn("You are wiring all error handlers to stop for any exception." +
//                    " This is useful for setup and troubleshooting, but unlikely to" +
//                    " be useful for long-term or bulk testing, as retryable errors" +
//                    " are normal in a busy system.");
//            this.realErrorResponse = this.retryableResponse = ErrorResponse.stop;
//        } else {
//            String realErrorsSpec = activityDef.getParams()
//                    .getOptionalString("realerrors").orElse(ErrorResponse.stop.toString());
//            this.realErrorResponse = ErrorResponse.valueOf(realErrorsSpec);
//
//            String retryableSpec = activityDef.getParams()
//                    .getOptionalString("retryable").orElse(ErrorResponse.retry.toString());
//
//            this.retryableResponse = ErrorResponse.valueOf(retryableSpec);
//        }
    }
}
