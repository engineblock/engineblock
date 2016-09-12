package io.engineblock.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * No CLI parser lib is useful for command structures, it seems. So we have this instead, which is good enough.
 * If something better is needed later, this can be replaced.
 */
public class EBCLIOptions {

    public static final String docoptFileName = "commandline.txt";
    private final static Logger logger = LoggerFactory.getLogger(EBCLIOptions.class);
    private static final String ACTIVITY = "activity";
    private static final String VERSION = "version";
    private static final String SCRIPT = "script";
    private static final String METRICS = "metrics";
    private static final String ACTIVITY_TYPES = "activitytypes";
    private static final String HELP = "help";
    private static final String METRICS_PREFIX = "metrics-prefix";
    private static final String REPORT_GRAPHITE_TO = "report-graphite-to";
    private static final String WANTS_VERBOSE_LOGGING = "-v";
    private static final String WANTS_VERBOSE_LOGGING_LONG = "--verbose";
    private static final Set<String> reserved_words = new HashSet<String>() {{
        addAll(
                Arrays.asList(
                        ACTIVITY, VERSION, SCRIPT, ACTIVITY_TYPES, HELP, METRICS_PREFIX, REPORT_GRAPHITE_TO
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
            String word = arglist.removeFirst();
            switch (word) {
                case ACTIVITY:
                    List<String> activitydef = new ArrayList<String>();
                    while (arglist.size() > 0 && !reserved_words.contains(arglist.peekFirst()) && arglist.peekFirst().contains("=")) {
                        activitydef.add(arglist.removeFirst());
                    }
                    cmdList.add(new Cmd(CmdType.activity, activitydef.stream().map(s -> s + ";").collect(Collectors.joining())));
                    break;
                case SCRIPT:
                    if (arglist.size() < 1) {
                        throw new InvalidParameterException("missing script name after script command");
                    }
                    if (reserved_words.contains(arglist.peekFirst())) {
                        throw new InvalidParameterException("script name may not be a reserved word, like '" + arglist.peekFirst() + "'");
                    }
                    if (arglist.peekFirst().contains("=")) {
                        throw new InvalidParameterException("script name must precede script arguments such as '" + arglist.peekFirst() + "'");
                    }

                    String scriptName = arglist.removeFirst();
                    Map<String, String> scriptParams = new LinkedHashMap<>();
                    while (arglist.size() > 0 && !reserved_words.contains(arglist.peekFirst())
                            && arglist.peekFirst().contains("=")) {
                        String[] split = arglist.removeFirst().split("=", 2);
                        scriptParams.put(split[0], split[1]);
                    }
                    cmdList.add(new Cmd(CmdType.valueOf(word), scriptName, scriptParams));
                    break;
                case VERSION:
                    wantsVersion = true;
                    break;
                case HELP:
                case "-h":
                case "--help":
                    if (arglist.peekFirst() == null) {
                        wantsBasicHelp = true;
                        logger.info("getting basic help");
                    } else {
                        wantsActivityHelp = true;
                        wantsActivityHelpFor = arglist.removeFirst();
                    }
                    break;
                case METRICS:
                    if (arglist.peekFirst() == null) {
                        throw new InvalidParameterException("activity type must follow metrics command");
                    }
                    wantsMetricsForActivity = arglist.removeFirst();
                    break;
                case REPORT_GRAPHITE_TO:
                    reportGraphiteTo = arglist.removeFirst();
                    break;
                case METRICS_PREFIX:
                    metricsPrefix = arglist.removeFirst();
                    break;
                case ACTIVITY_TYPES:
                    wantsActivityTypes = true;
                    break;
                case WANTS_VERBOSE_LOGGING:
                case WANTS_VERBOSE_LOGGING_LONG:
                    wantsConsoleLogging = true;
                    break;
                default:
                    throw new InvalidParameterException("unrecognized command:" + word);

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

    public String wantsReportGraphiteTo() {
        return reportGraphiteTo;
    }

    public String wantsMetricsPrefix() {
        return metricsPrefix;
    }

    public boolean wantsConsoleLogging() {
        return wantsConsoleLogging;
    }

    public String wantsMetricsForActivity() { return wantsMetricsForActivity; }

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

}
