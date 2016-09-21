package io.engineblock.cli;

import io.engineblock.activityapi.ActivityType;
import io.engineblock.core.ActivityDocInfo;
import io.engineblock.core.ActivityTypeFinder;
import io.engineblock.core.ScenariosResults;
import io.engineblock.script.MetricsMapper;
import io.engineblock.script.Scenario;
import io.engineblock.script.ScenariosExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class EBCLI {

    private static final Logger logger = LoggerFactory.getLogger(EBCLI.class);

    private String commandName;

    public EBCLI(String commandName) {
        this.commandName = commandName;
    }

    public static void main(String[] args) {
        EBCLI cli = new EBCLI("eb");
        cli.run(args);
    }

    public void run(String[] args) {
        EBCLIOptions options = new EBCLIOptions(args);

        if (options.wantsVersion()) {
            System.out.println(new VersionInfo().getVersion());
            System.out.println(new VersionInfo().getArtifactCoordinates());
            System.exit(0);
        }

        if (options.wantsActivityTypes()) {
            ActivityTypeFinder.instance().getAll().stream().map(ActivityType::getName).forEach(System.out::println);
            System.exit(0);
        }

        if (options.wantsBasicHelp()) {
            System.out.println(getBasicHelp());
            System.exit(0);
        }

        if (options.wantsMetricsForActivity() != null) {
            String metricsHelp = getMetricsHelpFor(
                    options.wantsMetricsForActivity(),
                    options.wantsMetricsForActivityNamed()
            );
            System.out.println("Available metric names for activity type " + options.wantsMetricsForActivity() + ":");
            System.out.println(metricsHelp);
            System.exit(0);
        }

        if (options.wantsActivityHelp()) {
            String activityHelpMarkdown = ActivityDocInfo.forActivityType(options.wantsActivityHelpFor());
            System.out.println(activityHelpMarkdown);
        }

        if (options.wantsConsoleLogging()) {
            ConsoleLogging.enableConsoleLogging();
        }

        if (options.getCommands().size() == 0) {
            System.out.println(getBasicHelp());
            System.exit(0);
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String sessionName = "scenario-" + timestamp;
        if (!options.getSessionName().isEmpty()) {
            sessionName = options.getSessionName();
        }

        ScenariosExecutor executor = new ScenariosExecutor("executor-" + sessionName, 1);

        Scenario scenario = new Scenario(sessionName);
        String script = EBCLIScriptAssembly.assembleScript(options);
        scenario.addScriptText(script);
        executor.execute(scenario);
        ScenariosResults scenariosResults = executor.awaitAllResults();
        //scenariosResults.reportSummaryTo(System.out);
        scenariosResults.reportToLog();
    }

    private String getBasicHelp() {
        String docoptFileName = "commandline.txt";
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
        basicHelp = basicHelp.replaceAll("PROG", commandName);
        return basicHelp;

    }

    private String getMetricsHelpFor(String activityType, String exampleActivityName) {
        String metrics = MetricsMapper.metricsDetail(activityType, exampleActivityName);
        return metrics;
    }
}
