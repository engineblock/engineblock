package io.engineblock.activitytypes.http;

import io.engineblock.activityapi.core.Action;
import io.engineblock.activityapi.core.ActionDispenser;

public class HttpActionDispenser implements ActionDispenser {
    private HttpActivity httpActivity;

    public HttpActionDispenser(HttpActivity httpActivity) {
        this.httpActivity = httpActivity;
    }

    @Override
    public Action getAction(int i) {
        return new HttpAction(httpActivity.getActivityDef(), i, httpActivity);
    }
}