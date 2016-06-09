package io.engineblock.cli;

import io.engineblock.activityapi.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class EMCLIScriptAssembly {
    private final static Logger logger = LoggerFactory.getLogger(EMCLIScriptAssembly.class);

    public static String assembleScript(EMCLIOptions options) {
        StringBuilder sb = new StringBuilder();
        for (EMCLIOptions.Cmd cmd : options.getCommands()) {
            switch (cmd.cmdType) {
                case activity:
                    // Sanity check that this can parse before using it
                    ActivityDef activityDef = ActivityDef.parseActivityDef(cmd.cmdSpec);
                    sb.append("scenario.start('" + cmd.cmdSpec + "');\n");
                    break;
                case script:
                    String scriptData = loadScript(cmd.cmdSpec);
                    sb.append(scriptData);
                    break;
            }
        }
        return sb.toString();

    }

    private static String loadScript(String cmdSpec) {
        String scriptData;

        try {
            logger.debug("Looking for " + new File(".").getCanonicalPath() + File.separator + cmdSpec);
        } catch (IOException ignored) {
        }
        InputStream resourceAsStream = EMCLIScriptAssembly.class.getClassLoader().getResourceAsStream(cmdSpec);
        if (resourceAsStream==null) {
                throw new RuntimeException("Unable to find " + cmdSpec);
        }
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            scriptData = buffer.lines().collect(Collectors.joining("\n"));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to buffer " + cmdSpec + ": " + t);
        }
        return scriptData;
    }
}
