/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package io.engineblock.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import io.engineblock.script.Scenario;
import org.slf4j.LoggerFactory;

public class ScenarioLogger {

    public static void start(Scenario scenario) {

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        //List<LoggerContextListener> copyOfListenerList = loggerContext.getCopyOfListenerList();

        ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
        ca.setContext(loggerContext);

        PatternLayoutEncoder ple = new PatternLayoutEncoder();
//        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(loggerContext);
        ple.start();

        String scenarioLog = scenario.getName()+".log";
        scenarioLog = scenarioLog.replaceAll("\\s","_");
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setFile(scenarioLog);
        fileAppender.setEncoder(ple);
        fileAppender.setContext(loggerContext);
        System.err.println("Logging to " + scenarioLog);
        fileAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger("ROOT");
        logger.addAppender(fileAppender);

        logger.setLevel(Level.INFO);
        logger.setAdditive(true); /* set to true if root should log too */

    }
}
