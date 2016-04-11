package com.metawiring.load.cli;

import com.metawiring.tools.activityapi.ActivityType;
import com.metawiring.load.core.ActivityDocInfo;
import com.metawiring.load.core.ActivityTypeFinder;
import com.metawiring.load.script.ScriptExecutor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class EMCLI {

    public static void main(String[] args) {
        EMCLIOptions options = new EMCLIOptions(args);

        if (options.wantsVersion()) {
            System.out.println(new VersionInfo().getVersion());
            System.out.println(new VersionInfo().getArtifactCoordinates());
            System.exit(0);
        }

        if (options.wantsActivityTypes()) {
            ActivityTypeFinder.getAll().stream().map(ActivityType::getName).forEach(System.out::println);
            System.exit(0);
        }

        if (options.wantsBasicHelp()) {
            String docoptFileName = "docopt.txt";
            InputStream resourceAsStream = EMCLI.class.getResourceAsStream(docoptFileName);
            if (resourceAsStream == null) {
                throw new RuntimeException("Unable to find " + docoptFileName + " in classpath.");
            }
            String basicHelp;
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resourceAsStream))) {
                basicHelp = buffer.lines().collect(Collectors.joining("\n"));
            } catch (Throwable t) {
                throw new RuntimeException("Unable to buffer " + docoptFileName + ": " + t);
            }
            System.out.println(basicHelp);
            System.exit(0);

        }

        if (options.wantsActivityHelp()) {
            ActivityDocInfo.forActivityType(options.wantsActivityHelpFor());
        }

        String scriptText = EMCLIScriptAssembly.assembleScript(options);
        runScript(scriptText);
    }

    private static void runScript(String scriptText) {
        ScriptExecutor executor = new ScriptExecutor();
        executor.addScriptText(scriptText);
        executor.run();
    }

}
