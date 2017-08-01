package io.engineblock.core;

import io.engineblock.activityapi.ActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

public class MarkdownDocInfo {
    private final static Logger logger = LoggerFactory.getLogger(MarkdownDocInfo.class);

    public static Optional<String> forHelpTopic(String topic) {
        String help = null;
        try {
            help = new MarkdownDocInfo().forActivityInstance(topic);
            return Optional.ofNullable(help);
        } catch (Exception e) {
            logger.debug("Did not find help topic for activity instance: "  + topic);
        }

        try {
            help = new MarkdownDocInfo().forResourceMarkdown(topic);
            return Optional.ofNullable(help);
        } catch (Exception e) {
            logger.debug("Did not find help topic for generic markdown file: " + topic + "(.md)");
        }

        return Optional.empty();

    }



    public String forResourceMarkdown(String s) {
        String docFileName = s + ".md";

        logger.info("loading docfile from path:" + docFileName);
        InputStream docStream = getClass().getClassLoader().getResourceAsStream(docFileName);
        if (docStream==null) {
            throw new RuntimeException("Unable to find docstream in classpath: " + docFileName);
        }
        String docInfo = "";
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(docStream))) {
            docInfo = buffer.lines().collect(Collectors.joining("\n"));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to buffer data from docstream: " + docFileName + ":" + t);
        }
        return docInfo;
    }

    public String forActivityInstance(String s) {
        ActivityType activityType = ActivityTypeFinder.instance().getOrThrow(s);
        return forResourceMarkdown(activityType.getName()+".md");
    }

}
