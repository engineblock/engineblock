package io.engineblock.cli;

import io.engineblock.activityapi.ActivityType;
import io.engineblock.core.ActivityDocInfo;
import io.engineblock.core.ActivityTypeFinder;
import io.engineblock.core.ScenariosResults;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.metrics.MetricReporters;
import io.engineblock.script.Scenario;
import io.engineblock.script.MetricsMapper;
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

        if (options.wantsBasicHelp()) {
            System.out.println(loadHelpFile("commandline.md"));
            System.exit(0);
        }

        if (options.wantsAdvancedHelp()) {
            System.out.println(loadHelpFile("advancedhelp.md"));
            System.exit(0);
        }

        if (options.wantsVersion()) {
            System.out.println(new VersionInfo().getVersion());
            System.out.println(new VersionInfo().getArtifactCoordinates());
            System.exit(0);
        }

        if (options.wantsActivityTypes()) {
            ActivityTypeFinder.instance().getAll().stream().map(ActivityType::getName).forEach(System.out::println);
            System.exit(0);
        }

        if (options.wantsActivityHelp()) {
            String activityHelpMarkdown = ActivityDocInfo.forActivityType(options.wantsActivityHelpFor());
            System.out.println(activityHelpMarkdown);
            System.exit(0);
        }

        if (options.wantsMetricsForActivity() != null) {
            String metricsHelp = getMetricsHelpFor(options.wantsMetricsForActivity());
            System.out.println("Available metric names for activity:" + options.wantsMetricsForActivity() + ":");
            System.out.println(metricsHelp);
            System.exit(0);
        }

        if (options.wantsReportGraphiteTo()!=null) {
            MetricReporters reporters = MetricReporters.getInstance();
            reporters.addRegistry("workloads", ActivityMetrics.getMetricRegistry());
            reporters.addGraphite(options.wantsReportGraphiteTo(),options.wantsMetricsPrefix());
            reporters.start(10,10);
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String sessionName = "scenario-" + timestamp;
        if (!options.getSessionName().isEmpty()) {
            sessionName = options.getSessionName();
        }

        for (EBCLIOptions.HistoConfig histoConfig : options.getHistoLoggerConfigs()) {
            ActivityMetrics.addHistoLogger(sessionName, histoConfig.pattern, histoConfig.file, histoConfig.interval);
        }

        ConsoleLogging.enableConsoleLogging(options.wantsConsoleLogLevel());
        // intentionally not shown for warn-only
        logger.info("console logging level is " + options.wantsConsoleLogLevel());

        if (options.getCommands().size() == 0) {
            System.out.println(loadHelpFile("commandline.md"));
            System.exit(0);
        }


        ScenariosExecutor executor = new ScenariosExecutor("executor-" + sessionName, 1);

        Scenario scenario = new Scenario(sessionName);
        String script = EBCLIScriptAssembly.assembleScript(options);
        if (options.wantsShowScript()) {
            System.out.println("// Script");
            System.out.println(script);
            System.exit(0);
        }

        scenario.addScriptText(script);
        executor.execute(scenario);
        ScenariosResults scenariosResults = executor.awaitAllResults();
        //scenariosResults.reportSummaryTo(System.out);
        scenariosResults.reportToLog();
    }

    private String loadHelpFile(String filename) {
        ClassLoader cl = getClass().getClassLoader();
        InputStream resourceAsStream = cl.getResourceAsStream(filename);
        if (resourceAsStream == null) {
            throw new RuntimeException("Unable to find " + filename + " in classpath.");
        }
        String basicHelp;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            basicHelp = buffer.lines().collect(Collectors.joining("\n"));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to buffer " + filename + ": " + t);
        }
        basicHelp = basicHelp.replaceAll("PROG", commandName);
        return basicHelp;

    }

    private String getMetricsHelpFor(String activityType) {
        String metrics = MetricsMapper.metricsDetail(activityType);
        return metrics;
    }
}
