package io.engineblock.cli;

import io.engineblock.activityapi.ActivityDef;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class EMCLIScriptAssembly {

    public static String assembleScript(EMCLIOptions options) {
        StringBuilder sb = new StringBuilder();
        for (EMCLIOptions.Cmd cmd : options.getCommands()) {
            switch (cmd.cmdType) {
                case activity:
                    // Sanity check that this can parse before using it
                    ActivityDef activityDef = ActivityDef.parseActivityDef(cmd.cmdSpec);
                    sb.append("sc.start('" + cmd.cmdSpec + "');\n");
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
        InputStream resourceAsStream = EMCLIScriptAssembly.class.getResourceAsStream(cmdSpec);
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            scriptData = buffer.lines().collect(Collectors.joining("\n"));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to buffer " + cmdSpec + ": " + t);
        }
        return scriptData;
    }
}
