package io.engineblock.cli;

import ch.qos.logback.classic.Level;
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
    public static final String docoptFileName = "commandline.md";

    // Discovery
    private static final String HELP = "--help";
    private static final String ADVANCED_HELP = "--advanced-help";
    private static final String METRICS = "--list-metrics";
    private static final String ACTIVITY_TYPES = "--list-activity-types";
    private static final String WANTS_VERSION_LONG = "--version";
    private static final String SHOW_SCRIPT = "--show-script";

    // Execution
    private static final String ACTIVITY = "activity";
    private static final String RUN_ACTIVITY = "run";
    private static final String START_ACTIVITY = "start";
    private static final String STOP_ACTIVITY = "stop";
    private static final String AWAIT_ACTIVITY = "await";
    private static final String WAIT_MILLIS = "waitmillis";

    // Execution Options
    private static final String SCRIPT = "script";
    private static final String SESSION_NAME = "--session-name";
    private static final String WANTS_INFO_CONSOLE_LOGGING = "-v";
    private static final String WANTS_DEBUG_CONSOLE_LOGGING = "-vv";
    private static final String WANTS_TRACE_CONSOLE_LOGGING = "-vvv";
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
    private String wantsMetricsForActivity;
    private boolean wantsAdvancedHelp = false;
    private String sessionName = "";
    private boolean showScript = false;
    private Level consoleLevel = Level.WARN;

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
                case SHOW_SCRIPT:
                    arglist.removeFirst();
                    showScript =true;
                    break;
                case ACTIVITY:
                    arglist.removeFirst();
                    arglist.addFirst("run");
                case START_ACTIVITY:
                case RUN_ACTIVITY:
                    Cmd activity = parseActivityCmd(arglist);
                    cmdList.add(activity);
                    break;
                case METRICS:
                    arglist.removeFirst();
                    arglist.addFirst("start");
                    Cmd introspectActivity = parseActivityCmd(arglist);
                    wantsMetricsForActivity=introspectActivity.cmdSpec;
                    break;
                case AWAIT_ACTIVITY:
                    String awaitCmdType = arglist.removeFirst();
                    String activityToAwait = readWordOrThrow(arglist,"activity alias to await");
                    assertNotParameter(activityToAwait);
                    assertNotReserved(activityToAwait);
                    Cmd awaitActivityCmd = new Cmd(CmdType.valueOf(awaitCmdType),activityToAwait);
                    cmdList.add(awaitActivityCmd);
                    break;
                case STOP_ACTIVITY:
                    String stopCmdType = readWordOrThrow(arglist,"stop command");
                    String activityToStop = readWordOrThrow(arglist,"activity alias to await");
                    assertNotParameter(activityToStop);
                    assertNotReserved(activityToStop);
                    Cmd stopActivityCmd = new Cmd(CmdType.valueOf(stopCmdType),activityToStop);
                    cmdList.add(stopActivityCmd);
                    break;
                case WAIT_MILLIS:
                    String waitMillisCmdType = readWordOrThrow(arglist,"wait millis");
                    String millisCount = readWordOrThrow(arglist, "millis count");
                    Long.parseLong(millisCount);
                    Cmd awaitMillisCmd = new Cmd(CmdType.valueOf(waitMillisCmdType),millisCount);
                    cmdList.add(awaitMillisCmd);
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
                case "help":
                    arglist.removeFirst();
                    if (arglist.peekFirst() == null) {
                        wantsBasicHelp = true;
                        logger.info("getting basic help");
                    } else {
                        wantsActivityHelp = true;
                        wantsActivityHelpFor = arglist.removeFirst();
                    }
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
                case WANTS_DEBUG_CONSOLE_LOGGING:
                    consoleLevel = Level.DEBUG;
                    arglist.removeFirst();
                    break;
                case WANTS_INFO_CONSOLE_LOGGING:
                    consoleLevel = Level.INFO;
                    arglist.removeFirst();
                    break;
                case WANTS_TRACE_CONSOLE_LOGGING:
                    consoleLevel = Level.TRACE;
                    arglist.removeFirst();
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
                        throw new InvalidParameterException("unrecognized option:" + word);
                    }
            }
        }
    }

    public List<Cmd> getCommands() {
        return cmdList;
    }

    public boolean wantsShowScript() {
        return showScript;
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

    public String wantsMetricsForActivity() {
        return wantsMetricsForActivity;
    }

    public String getSessionName() {
        return sessionName;
    }

    public Level wantsConsoleLogLevel() {
        return consoleLevel;
    }

    public static enum CmdType {
        start,
        run,
        stop,
        await,
        script,
        waitmillis,
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
        return new Cmd(CmdType.valueOf(cmdType), activitydef.stream().map(s -> s + ";").collect(Collectors.joining()));
    }


}
