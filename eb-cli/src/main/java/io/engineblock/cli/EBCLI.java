package io.engineblock.cli;

import io.engineblock.activityapi.core.ActivityType;
import io.engineblock.activityapi.cyclelog.outputs.cyclelog.CycleLogDumperUtility;
import io.engineblock.activityapi.cyclelog.outputs.cyclelog.CycleLogImporterUtility;
import io.engineblock.activityapi.input.InputType;
import io.engineblock.activityapi.output.OutputType;
import io.engineblock.core.MarkdownDocInfo;
import io.engineblock.core.ScenarioLogger;
import io.engineblock.core.ScenariosResults;
import io.engineblock.core.ShutdownManager;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.metrics.HistoLogChartGenerator;
import io.engineblock.metrics.MetricReporters;
import io.engineblock.script.MetricsMapper;
import io.engineblock.script.Scenario;
import io.engineblock.script.ScenariosExecutor;
import io.virtdata.apps.MainApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class EBCLI {

    private static final Logger logger = LoggerFactory.getLogger(EBCLI.class);
    private static final String CHART_HDR_LOG_NAME = "hdrdata-for-chart.log";

    private String commandName;

    public EBCLI(String commandName) {
        this.commandName = commandName;
    }

    public static void main(String[] args) {
        EBCLI cli = new EBCLI("eb");
        cli.run(args);
    }

    public void run(String[] args) { 
        if (args.length>0 && args[0].toLowerCase().equals("virtdata")) {
            MainApp.main(Arrays.copyOfRange(args,1,args.length));
            System.exit(0);
        }

        EBCLIOptions options = new EBCLIOptions(args);

        if (options.wantsBasicHelp()) {
            System.out.println(loadHelpFile("commandline.md"));
            System.exit(0);
        }

        if (options.wantsAdvancedHelp()) {
            System.out.println(loadHelpFile("cli_scripting.md"));
            System.exit(0);
        }

        if (options.wantsVersion()) {
            System.out.println(new VersionInfo().getVersion());
            System.out.println(new VersionInfo().getArtifactCoordinates());
            System.exit(0);
        }

        if (options.wantsActivityTypes()) {
            ActivityType.FINDER.getAll().stream().map(ActivityType::getName).forEach(System.out::println);
            System.exit(0);
        }

        if (options.wantsInputTypes()) {
            InputType.FINDER.getAll().stream().map(InputType::getName).forEach(System.out::println);
            System.exit(0);
        }

        if (options.wantsMarkerTypes()) {
            OutputType.FINDER.getAll().stream().map(OutputType::getName).forEach(System.out::println);
            System.exit(0);
        }

        if (options.wantsToDumpCyclelog()) {
            CycleLogDumperUtility.main(options.getCycleLogExporterOptions());
            System.exit(0);
        }

        if (options.wantsToImportCycleLog()) {
            CycleLogImporterUtility.main(options.getCyclelogImportOptions());
            System.exit(0);
        }

        if (options.wantsTopicalHelp()) {
            Optional<String> helpDoc = MarkdownDocInfo.forHelpTopic(options.wantsTopicalHelpFor());
            System.out.println(helpDoc.orElseThrow(
                    () -> new RuntimeException("No help could be found for " + options.wantsTopicalHelpFor())
            ));
            System.exit(0);
        }

        if (options.wantsMetricsForActivity() != null) {
            String metricsHelp = getMetricsHelpFor(options.wantsMetricsForActivity());
            System.out.println("Available metric names for activity:" + options.wantsMetricsForActivity() + ":");
            System.out.println(metricsHelp);
            System.exit(0);
        }

        String reportGraphiteTo = options.wantsReportGraphiteTo();
        if (options.wantsDockerMetrics()){
            logger.info("Docker metrics is enabled. Docker must be installed for this to work");
            DockerMetricsHelper dmh= new DockerMetricsHelper();
            dmh.startMetrics();
            logger.info("Docker Containers are started, for grafana and prometheus, hit" +
                    "these urls in your browser: http://<host>:3000 and http://<host>:9090" +
                    "the default grafana creds are admin/admin");
            if (reportGraphiteTo != null){
                logger.warn(String.format("Docker metrics are enabled (--docker-metrics)" +
                    " but graphite reporting (--report-graphite-to) is set to %s \n" +
                    "usually only one of the two is configured.",
                    reportGraphiteTo));
            }else{
                //TODO: is this right?
                logger.info("Setting graphite reporting to localhost");
                reportGraphiteTo = "localhost:9109";
            }
        }

        if (reportGraphiteTo != null || options.wantsReportCsvTo() != null) {
            MetricReporters reporters = MetricReporters.getInstance();
            reporters.addRegistry("workloads", ActivityMetrics.getMetricRegistry());

            if (reportGraphiteTo != null) {
                reporters.addGraphite(reportGraphiteTo,  options.wantsMetricsPrefix());
            }
            if (options.wantsReportCsvTo() != null) {
                reporters.addCSVReporter(options.wantsReportCsvTo(), options.wantsMetricsPrefix());
            }
            reporters.start(10, options.getReportInterval());
        }

        String sessionName = new SessionNamer().format(options.getSessionName());

        if (options.wantsEnableChart()){
            logger.info("Charting enabled");
            if (options.getHistoLoggerConfigs().size() == 0) {
                logger.info("Adding default histologger configs");
                String pattern = ".*";
                String file = CHART_HDR_LOG_NAME;
                String interval = "1s";
                options.setHistoLoggerConfigs(pattern, file, interval);
            }
        }

        for (EBCLIOptions.LoggerConfig histoLogger : options.getHistoLoggerConfigs()) {
            ActivityMetrics.addHistoLogger(sessionName, histoLogger.pattern, histoLogger.file, histoLogger.interval);
        }
        for (EBCLIOptions.LoggerConfig statsLogger : options.getStatsLoggerConfigs()) {
            ActivityMetrics.addStatsLogger(sessionName, statsLogger.pattern, statsLogger.file, statsLogger.interval);
        }
        for (EBCLIOptions.LoggerConfig classicConfigs : options.getClassicHistoConfigs()) {
            ActivityMetrics.addClassicHistos(sessionName, classicConfigs.pattern, classicConfigs.file, classicConfigs.interval);
        }

        ConsoleLogging.enableConsoleLogging(options.wantsConsoleLogLevel(), options.getConsoleLoggingPattern());
        // intentionally not shown for warn-only
        logger.info("console logging level is " + options.wantsConsoleLogLevel());

        if (options.getCommands().size() == 0) {
            System.out.println(loadHelpFile("commandline.md"));
            System.exit(0);
        }

        ScenariosExecutor executor = new ScenariosExecutor("executor-" + sessionName, 1);

        Scenario scenario = new Scenario(sessionName, options.getProgressSpec());
        EBCLIScriptAssembly.ScriptData scriptData = EBCLIScriptAssembly.assembleScript(options);
        if (options.wantsShowScript()) {
            System.out.println("// Rendered Script");
            System.out.println(scriptData.getScriptParamsAndText());
            System.exit(0);
        }

        if (options.wantsEnableChart()){
            logger.info("Charting enabled");
            scenario.enableCharting();
        } else{
            logger.info("Charting disabled");
        }

        scenario.addScenarioScriptParams(scriptData.getScriptParams());
        scenario.addScriptText(scriptData.getScriptTextIgnoringParams());
        ScenarioLogger sl = new ScenarioLogger(scenario)
                .setLogDir(options.getLogsDirectory())
                .setMaxLogs(options.getLogsMax())
                .setLevel(options.getLogsLevel())
                .setLogLevelOverrides(options.getLogLevelOverrides())
                .start();

        executor.execute(scenario, sl);
        ScenariosResults scenariosResults = executor.awaitAllResults();
        ActivityMetrics.closeMetrics(options.wantsEnableChart());
        scenariosResults.reportToLog();
        ShutdownManager.shutdown();

        if (scenariosResults.hasError()) {
            System.exit(2);
        } else {
            System.exit(0);
        }
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
