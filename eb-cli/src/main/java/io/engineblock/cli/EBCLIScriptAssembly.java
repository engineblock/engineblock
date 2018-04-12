package io.engineblock.cli;

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.util.EngineBlockFiles;
import io.engineblock.util.StrInterpolater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EBCLIScriptAssembly {
    private final static Logger logger = LoggerFactory.getLogger(EBCLIScriptAssembly.class);

    public static ScriptData assembleScript(EBCLIOptions options) {
        StringBuilder sb = new StringBuilder();
        Map<String,String> params = new HashMap<>();
        for (EBCLIOptions.Cmd cmd : options.getCommands()) {
            switch (cmd.cmdType) {
                case script:
                    sb.append("// from CLI as ").append(cmd).append("\n");
                    ScriptData scriptData = loadScript(cmd);
                    if (options.getCommands().size()==1) {
                        sb.append(scriptData.getScriptTextIgnoringParams());
                        params = scriptData.getScriptParams();
                    } else {
                        sb.append(scriptData.getScriptParamsAndText());
                    }
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

        return new ScriptData(sb.toString(), params);
    }

    private static ScriptData loadScript(EBCLIOptions.Cmd cmd) {
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
        return new ScriptData(scriptData,cmd.getCmdArgs());
    }

    public static class ScriptData {
        private final String scriptText;
        private final Map<String, String> scriptParams;

        public ScriptData(String scriptText, Map<String,String> scriptParams) {

            this.scriptText = scriptText;
            this.scriptParams = scriptParams;
        }

        public String getScriptTextIgnoringParams() {
            return scriptText;
        }

        public Map<String, String> getScriptParams() {
            return scriptParams;
        }

        public String getScriptParamsAndText() {
            return "// params:\n" + toJSON(scriptParams) + "\n// script:\n" + scriptText;
        }
    }

    private static String toJSON(Map<?,?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("params={\n");
        map.forEach((k,v) -> { sb.append(" '").append(k.toString()).append("',\n"); });
        sb.setLength(sb.length()-1);
        sb.append("};\n");
        return sb.toString();
    }
}
