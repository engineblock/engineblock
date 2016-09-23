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
                case script:
                    String scriptData = loadScript(cmd);
                    sb.append("// from CLI as ").append(cmd).append("\n");
                    sb.append(scriptData);
                    break;
                case start: // start activity
                case run: // run activity
                    // Sanity check that this can parse before using it
                    ActivityDef activityDef = ActivityDef.parseActivityDef(cmd.cmdSpec);
                    sb.append("// from CLI as ").append(cmd).append("\n")
                            .append("scenario.").append(cmd.cmdType.toString()).append("(\"")
                            .append(cmd.cmdSpec)
                            .append("\");\n");
                    break;
                case await: // await activity
                    sb.append("// from CLI as ").append(cmd).append("\n");
                    sb.append("scenario.awaitActivity(\"").append(cmd.cmdSpec).append("\");\n");
                    break;
                case stop: // stop activity
                    sb.append("// from CLI as ").append(cmd).append("\n");
                    sb.append("scenario.stop(\"").append(cmd.cmdSpec).append("\");\n");
                    break;
                case waitmillis:
                    sb.append("// from CLI as ").append(cmd).append("\n");
                    sb.append("scenario.waitMillis(").append(cmd.cmdSpec).append(");\n");
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
