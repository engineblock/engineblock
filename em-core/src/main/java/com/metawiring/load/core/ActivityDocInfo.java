package com.metawiring.load.core;

import com.metawiring.tools.activityapi.ActivityType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ActivityDocInfo {

    public static String forActivityType(String s) {
        ActivityType activityType = ActivityTypeFinder.get(s);
        String docFileName = activityType.getName() + ".md";
        InputStream docStream = activityType.getClass().getResourceAsStream(docFileName);
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
