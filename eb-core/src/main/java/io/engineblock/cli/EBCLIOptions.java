package io.engineblock.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * No CLI parser lib is useful for command structures, it seems. So we have this instead, which is good enough.
 * If something better is needed later, this can be replaced.
 */
public class EBCLIOptions {

    private final static Logger logger = LoggerFactory.getLogger(EBCLIOptions.class);

    public static final String docoptFileName = "docopt.txt";

    private static final String ACTIVITY = "activity";
    private static final String VERSION = "version";
    private static final String SCRIPT = "script";
    private static final String ACTIVITY_TYPES = "activitytypes";
    private static final String HELP = "help";

    private LinkedList<Cmd> cmdList = new LinkedList<>();
    private boolean wantsVersion = false;
    private boolean wantsActivityHelp = false;
    private String wantsActivityHelpFor;
    private boolean wantsActivityTypes = false;
    private boolean wantsBasicHelp = false;

    EBCLIOptions(String[] args) {
        parse(args);
    }

    private void parse(String[] args) {

        LinkedList<String> arglist = new LinkedList<String>() {{
            addAll(Arrays.asList(args));
        }};

        if (arglist.peekFirst()==null) {
            wantsBasicHelp = true;
            return;
        }

        while (arglist.peekFirst() != null) {
            String word = arglist.removeFirst();
            switch (word) {
                case ACTIVITY:
                case SCRIPT:
                    cmdList.add(new Cmd(CmdType.valueOf(word),arglist.removeFirst()));
                    break;
                case VERSION:
                    wantsVersion = true;
                    break;
                case HELP:
                case "-h":
                case "--help":
                    if (arglist.peekFirst()==null) {
                        wantsBasicHelp = true;
                        logger.info("getting basic help");
                    } else {
                        wantsActivityHelp = true;
                        wantsActivityHelpFor = arglist.removeFirst();
                    }
                    break;
                case ACTIVITY_TYPES:
                    wantsActivityTypes = true;
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

    public static enum CmdType {
        activity,
        script
    }

    public static class Cmd {
        public CmdType cmdType;
        public String cmdSpec;
        public Cmd(CmdType cmdType, String cmdSpec) {
            this.cmdSpec = cmdSpec; this.cmdType = cmdType;
        }
        public String getCmdSpec() { return cmdSpec; }
        public CmdType getCmdType() { return cmdType; }
        public String toString() { return "type:" + cmdType + ";spec=" + cmdSpec; }
    }

}
