/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package io.engineblock.script;

import ch.qos.logback.classic.Logger;
import io.engineblock.core.Result;
import io.engineblock.core.ScenarioController;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class Scenario implements Callable<Result> {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(Scenario.class);
    private static final ScriptEngineManager engineManager = new ScriptEngineManager();

    private ScriptEngine nashorn;

    private final List<String> scripts = new ArrayList<>();

    private ScenarioController scenarioController;
    private ScriptEnv scriptEnv;
    private String name;
    private Logger scenarioLogger;
    private BufferAppender bufferAppender;

    public Scenario(String name) {
        this.name = name;
        init();
    }

    public Scenario addScriptText(String scriptText) {
        scripts.add(scriptText);
        return this;
    }


    public Scenario addScriptFiles(String... args) {
        for (String scriptFile : args) {
            Path scriptPath = Paths.get(scriptFile);
            byte[] bytes = new byte[0];
            try {
                bytes = Files.readAllBytes(scriptPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            Charset utf8 = Charset.forName("UTF8");
            String scriptData = utf8.decode(bb).toString();
            addScriptText(scriptData);
        }
        return this;
    }

    private void init() {
        nashorn = engineManager.getEngineByName("nashorn");

        scriptEnv = new ScriptEnv(scenarioController);
        nashorn.setContext(scriptEnv);

        scenarioController = new ScenarioController();
        nashorn.put("scenario", scenarioController);

        nashorn.put("activities", new ScenarioBindings(scenarioController));

//        bufferAppender = new BufferAppender<ILoggingEvent>();
//        bufferAppender.setName("scenario-" + getName());
//        bufferAppender.setContext(scenarioLogger.getLoggerContext());
//        scenarioLogger = (Logger) LoggerFactory.getLogger("scenario-" + getName());
//        scenarioLogger.addAppender(bufferAppender);
//        nashorn.put("logger", scenarioLogger);

    }

    public void run() {

        logger.info("Running control script for " + getName() + ".");
        for (String script : scripts) {
            try {
                Object result = nashorn.eval(script);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        }
        logger.info("Shutting down executors.");
        scenarioController.awaitCompletion(864000000);

    }

    public Result call() {
        run();
        String iolog = scriptEnv.getTimedLog().stream().collect(Collectors.joining());
        return new Result(iolog);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scenario scenario = (Scenario) o;
        return getName() != null ? getName().equals(scenario.getName()) : scenario.getName() == null;

    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }

    public String getName() {
        return name;
    }

    public ScenarioController getScenarioController() {
        return scenarioController;
    }

    public String getScriptText() {
        return scripts.stream().collect(Collectors.joining());
    }

    public Optional<List<String>> getIOLog() {
        return Optional.ofNullable(scriptEnv)
                .map(ScriptEnvBuffer::getTimedLog);
    }

    public Optional<BufferAppender> getLogBuffer() {
        return Optional.ofNullable(bufferAppender);
    }
}

