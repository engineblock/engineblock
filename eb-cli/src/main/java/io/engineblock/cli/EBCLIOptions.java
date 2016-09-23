package io.engineblock.cli;

import io.engineblock.util.EngineBlockFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * No CLI parser lib is useful for command structures, it seems. So we have this instead, which is good enough.
 * If something better is needed later, this can be replaced.
 */
public class EBCLIOptions {

    private final static Logger logger = LoggerFactory.getLogger(EBCLIOptions.class);
    public static final String docoptFileName = "commandline.txt";

    // Discovery
    private static final String HELP = "--help";
    private static final String ADVANCED_HELP = "--advanced-help";
    private static final String METRICS = "--list-metrics";
    private static final String ACTIVITY_TYPES = "--list-activity-types";
    private static final String WANTS_VERSION_LONG = "--version";

    // Execution
    private static final String ACTIVITY = "activity";

    // Execution Options
    private static final String SCRIPT = "script";
    private static final String SESSION_NAME = "--session-name";
    private static final String WANTS_VERBOSE_LOGGING_LONG = "--verbose";
    private static final String WANTS_VERBOSE_LOGGING = "-v";
    private static final String REPORT_GRAPHITE_TO = "--report-graphite-to";
    private static final String METRICS_PREFIX = "--metrics-prefix";

    private static final Set<String> reserved_words = new HashSet<String>() {{
        addAll(
                Arrays.asList(
                        ACTIVITY, SCRIPT, ACTIVITY_TYPES, HELP, METRICS_PREFIX, REPORT_GRAPHITE_TO
                )
        );
    }};

    private LinkedList<Cmd> cmdList = new LinkedList<>();
    private boolean wantsVersion = false;
    private boolean wantsActivityHelp = false;
    private String wantsActivityHelpFor;
    private boolean wantsActivityTypes = false;
    private boolean wantsBasicHelp = false;
    private String reportGraphiteTo = "";
    private String metricsPrefix = "engineblock.";
    private boolean wantsConsoleLogging = false;
    private String wantsMetricsForActivity;
    private boolean wantsAdvancedHelp = false;
    private String sessionName = "";

    EBCLIOptions(String[] args) {
        parse(args);
    }

    private void parse(String[] args) {

        LinkedList<String> arglist = new LinkedList<String>() {{
            addAll(Arrays.asList(args));
        }};

        if (arglist.peekFirst() == null) {
            wantsBasicHelp = true;
            return;
        }

        while (arglist.peekFirst() != null) {
            String word = arglist.peekFirst();
            switch (word) {
                case ACTIVITY:
                    Cmd activity = parseActivityCmd(arglist);
                    cmdList.add(activity);
                    break;
                case SCRIPT:
                    Cmd cmd = parseScriptCmd(arglist);
                    cmdList.add(cmd);
                    break;
                case SESSION_NAME:
                    arglist.removeFirst();
                    sessionName = readWordOrThrow(arglist, "a session name");
                    break;
                case WANTS_VERSION_LONG:
                    arglist.removeFirst();
                    wantsVersion = true;
                    break;
                case ADVANCED_HELP:
                    arglist.removeFirst();
                    wantsAdvancedHelp = true;
                    break;
                case HELP:
                case "-h":
                    arglist.removeFirst();
                    if (arglist.peekFirst() == null) {
                        wantsBasicHelp = true;
                        logger.info("getting basic help");
                    } else {
                        wantsActivityHelp = true;
                        wantsActivityHelpFor = arglist.removeFirst();
                    }
                    break;
                case METRICS:
                    arglist.removeFirst();
                    wantsMetricsForActivity = readWordOrThrow(arglist, "activity type");
                    break;
                case REPORT_GRAPHITE_TO:
                    arglist.removeFirst();
                    reportGraphiteTo = arglist.removeFirst();
                    break;
                case METRICS_PREFIX:
                    arglist.removeFirst();
                    metricsPrefix = arglist.removeFirst();
                    break;
                case ACTIVITY_TYPES:
                    arglist.removeFirst();
                    wantsActivityTypes = true;
                    break;
                case WANTS_VERBOSE_LOGGING:
                case WANTS_VERBOSE_LOGGING_LONG:
                    arglist.removeFirst();
                    wantsConsoleLogging = true;
                    break;
                default:
                    Optional<InputStream> optionalScript =
                            EngineBlockFiles.findOptionalStreamOrFile(word, "js", "scripts/auto");
                    if (optionalScript.isPresent()) {
                        arglist.removeFirst();
                        arglist.addFirst("scripts/auto/" + word);
                        arglist.addFirst("script");
                        Cmd script = parseScriptCmd(arglist);
                        cmdList.add(script);
                    } else {
                        throw new InvalidParameterException("unrecognized command:" + word);
                    }


            }
        }
    }

    public List<Cmd> getCommands() {
        return cmdList;
    }

    public boolean wantsVersion() {
        return wantsVersion;
    }

    public boolean wantsActivityTypes() {
        return wantsActivityTypes;
    }

    public boolean wantsActivityHelp() {
        return wantsActivityHelp;
    }

    public String wantsActivityHelpFor() {
        return wantsActivityHelpFor;
    }

    public boolean wantsBasicHelp() {
        return wantsBasicHelp;
    }

    public boolean wantsAdvancedHelp() {
        return wantsAdvancedHelp;
    }

    public String wantsReportGraphiteTo() {
        return reportGraphiteTo;
    }

    public String wantsMetricsPrefix() {
        return metricsPrefix;
    }

    public boolean wantsConsoleLogging() {
        return wantsConsoleLogging;
    }

    public String wantsMetricsForActivity() {
        return wantsMetricsForActivity;
    }

    public String getSessionName() {
        return sessionName;
    }

    public static enum CmdType {
        activity,
        script
    }

    public static class Cmd {
        public CmdType cmdType;
        public String cmdSpec;
        public Map<String, String> cmdArgs;

        public Cmd(CmdType cmdType, String cmdSpec) {
            this.cmdSpec = cmdSpec;
            this.cmdType = cmdType;
        }

        public Cmd(CmdType cmdType, String cmdSpec, Map<String, String> cmdArgs) {
            this(cmdType, cmdSpec);
            this.cmdArgs = cmdArgs;
        }

        public String getCmdSpec() {
            return cmdSpec;
        }

        public CmdType getCmdType() {
            return cmdType;
        }

        public Map<String, String> getCmdArgs() {
            return cmdArgs;
        }

        public String toString() {
            return "type:" + cmdType + ";spec=" + cmdSpec
                    + ((cmdArgs != null) ? "cmdArgs" + cmdArgs.toString() : "");
        }
    }

    private void assertNotParameter(String scriptName) {
        if (scriptName.contains("=")) {
            throw new InvalidParameterException("script name must precede script arguments");
        }

    }

    private void assertNotReserved(String name) {
        if (reserved_words.contains(name)) {
            throw new InvalidParameterException(name + " is a reserved word and may not be used here.");
        }
    }

    private String readOptionally(LinkedList<String> argList) {
        return argList.pollFirst();
    }

    private String readWordOrThrow(LinkedList<String> arglist, String required) {
        if (arglist.peekFirst() == null) {
            throw new InvalidParameterException(required + " not found");
        }
        return arglist.removeFirst();
    }

    private Cmd parseScriptCmd(LinkedList<String> arglist) {
        String cmdType = arglist.removeFirst();
        String scriptName = readWordOrThrow(arglist, "script name");
        assertNotReserved(scriptName);
        assertNotParameter(scriptName);
        Map<String, String> scriptParams = new LinkedHashMap<>();
        while (arglist.size() > 0 && !reserved_words.contains(arglist.peekFirst())
                && arglist.peekFirst().contains("=")) {
            String[] split = arglist.removeFirst().split("=", 2);
            scriptParams.put(split[0], split[1]);
        }
        return new Cmd(CmdType.script, scriptName, scriptParams);
    }

    private Cmd parseActivityCmd(LinkedList<String> arglist) {
        String cmdType = arglist.removeFirst();
        List<String> activitydef = new ArrayList<String>();
        while (arglist.size() > 0 &&
                !reserved_words.contains(arglist.peekFirst())
                && arglist.peekFirst().contains("=")) {
            activitydef.add(arglist.removeFirst());
        }
        return new Cmd(CmdType.activity, activitydef.stream().map(s -> s + ";").collect(Collectors.joining()));
    }


}
