package io.engineblock.cli;

import io.engineblock.activityapi.ActivityType;
import io.engineblock.core.ActivityDocInfo;
import io.engineblock.core.ActivityTypeFinder;
import io.engineblock.core.Result;
import io.engineblock.script.Scenario;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class EMCLI {

    public static void main(String[] args) {
        EMCLI cli = new EMCLI();
        cli.run(args);
    }

    public EMCLI() {}

    public void run(String[] args) {
        EMCLIOptions options = new EMCLIOptions(args);

        if (options.wantsVersion()) {
            System.out.println(new VersionInfo().getVersion());
            System.out.println(new VersionInfo().getArtifactCoordinates());
            System.exit(0);
        }

        if (options.wantsActivityTypes()) {
            ActivityTypeFinder.get().getAll().stream().map(ActivityType::getName).forEach(System.out::println);
            System.exit(0);
        }

        if (options.wantsBasicHelp()) {
            String docoptFileName = "docopt.txt";
            ClassLoader cl = getClass().getClassLoader();
            InputStream resourceAsStream = cl.getResourceAsStream(docoptFileName);
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
            String activityHelpMarkdown = ActivityDocInfo.forActivityType(options.wantsActivityHelpFor());
            System.out.println(activityHelpMarkdown);
        }

        String scriptText = EMCLIScriptAssembly.assembleScript(options);
        runScript(scriptText);
    }

    private static void runScript(String scriptText) {
        Scenario executor = new Scenario();
        executor.addScriptText(scriptText);
        Result result = executor.call();
        result.reportTo(System.out);
    }

}
