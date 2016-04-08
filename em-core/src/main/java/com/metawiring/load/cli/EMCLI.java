package com.metawiring.load.cli;

import com.metawiring.load.activityapi.ActivityType;
import com.metawiring.load.core.ActivityTypeFinder;
import com.metawiring.load.script.ScriptExecutor;

import java.util.List;

public class EMCLI {

    public static void main(String[] args) {
        EMCLIOptions options = EMCLIOptions.parse(args);

        if (options.wantsVersion()) {
            System.out.println(new VersionInfo().getVersion());
            System.exit(0);
        }

        if (options.wantsFullVersion()) {
            System.out.println(new VersionInfo().getArtifactCoordinates());
            System.exit(0);
        }

        if (options.wantsActivityTypes()) {
            ActivityTypeFinder.getAll().stream().map(ActivityType::getName).forEach(System.out::println);
        }

        runScripts(options.getScripts());
    }

    private static void runScripts(List<String> scripts) {
        ScriptExecutor executor = new ScriptExecutor();
        executor.addScriptFiles(scripts.toArray(new String[0]));
        executor.run();
    }

}
