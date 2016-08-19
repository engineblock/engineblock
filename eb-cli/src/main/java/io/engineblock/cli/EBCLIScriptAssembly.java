package io.engineblock.cli;

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.util.EngineBlockFiles;
import io.engineblock.util.StrInterpolater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.stream.Collectors;

public class EBCLIScriptAssembly {
    private final static Logger logger = LoggerFactory.getLogger(EBCLIScriptAssembly.class);

    public static String assembleScript(EBCLIOptions options) {
        StringBuilder sb = new StringBuilder();
        for (EBCLIOptions.Cmd cmd : options.getCommands()) {
            switch (cmd.cmdType) {
                case activity:
                    // Sanity check that this can parse before using it
                    ActivityDef activityDef = ActivityDef.parseActivityDef(cmd.cmdSpec);
                    sb.append("scenario.start(\"" + cmd.cmdSpec + "\");\n");
                    break;
                case script:
                    String scriptData = loadScript(cmd);
                    sb.append(scriptData);
                    break;
            }
        }
        return sb.toString();

    }

    private static String loadScript(EBCLIOptions.Cmd cmd) {
        String scriptData;

        try {
            logger.debug("Looking for " + new File(".").getCanonicalPath() + File.separator + cmd.cmdSpec);
        } catch (IOException ignored) {
        }

        InputStream resourceAsStream = EngineBlockFiles.findRequiredStreamOrFile(cmd.cmdSpec, "js", "scripts");

        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            scriptData = buffer.lines().collect(Collectors.joining("\n"));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to buffer " + cmd.cmdSpec + ": " + t);
        }
        StrInterpolater interpolater = new StrInterpolater(cmd.getCmdArgs());
        scriptData = interpolater.apply(scriptData);
        return scriptData;
    }
}
