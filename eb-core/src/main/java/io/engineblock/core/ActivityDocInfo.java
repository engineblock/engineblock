package io.engineblock.core;

import io.engineblock.activityapi.ActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ActivityDocInfo {
    private final static Logger logger = LoggerFactory.getLogger(ActivityDocInfo.class);

    public static String forActivityType(String s) {
        return new ActivityDocInfo().forActivityInstance(s);
    }

    public String forActivityInstance(String s) {
        ActivityType activityType = ActivityTypeFinder.instance().getOrThrow(s);
//        String docFileName = activityType.getClass().getPackage().getName().replaceAll("\\.", File.separator) + File.separator + activityType.getName() + ".md";
        String docFileName = activityType.getName() + ".md";

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

}
