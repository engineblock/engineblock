package com.metawiring.load.cli;

import com.metawiring.load.activityapi.ActivityDef;
import com.metawiring.load.config.ActivityDefImpl;
import org.docopt.Docopt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

public class EMCLIOptions {

    private final static Logger logger = LoggerFactory.getLogger(EMCLIOptions.class);

    public static final String docoptFileName = "docopt.txt";
    private static final String ACTIVITY = "--activity";
    private static final String VERSION = "-v";
    private static final String SCRIPT = "<script>";
    private static final String ACTIVITY_TYPES = "--activity-types";

    private final Map<String, Object> optmap;

    private EMCLIOptions(Map<String, Object> parsedOptions) {
        this.optmap = parsedOptions;
    }

    public static EMCLIOptions parse(String[] args) {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(docoptFileName);
        if (resourceAsStream == null) {
            throw new RuntimeException("Unable to find " + docoptFileName + " in classpath.");
        }
        Docopt docopt = new Docopt(resourceAsStream);
        Map<String, Object> parsedOptions = docopt.parse(args);
        return new EMCLIOptions(parsedOptions);
    }

    public List<ActivityDef> getActivities() {
        List<ActivityDef> activityDefs = new ArrayList<>();
        getStringList(ACTIVITY)
                .stream().map(ActivityDefImpl::parseActivityDef)
                .forEach(activityDefs::add);
        return activityDefs;
    }

    public boolean wantsFullVersion() {
        return (getInt(VERSION) == 2);
    }

    public boolean wantsVersion() {
        return (getInt(VERSION) == 1);
    }

    public List<String> getScripts() {
        List<String> scripts = new ArrayList<String>();
        scripts.addAll(getStringList(SCRIPT));
        return scripts;
    }

    private int getInt(String name) {
        return (Integer) optmap.get(name);
    }

    private List<String> getStringList(String name) {
        List<String> list = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        Optional<List<Object>> o = Optional.ofNullable((List<Object>) optmap.get(name));
        o.orElse(new ArrayList<Object>())
                .stream().forEach(s -> list.add((String) s));
        return list;
    }

    private boolean getBoolean(String name) {
        return (optmap.get(name)!=null && ((Boolean) optmap.get(name)));
    }

    public boolean wantsActivityTypes() {
        return getBoolean(ACTIVITY_TYPES);
    }
}
