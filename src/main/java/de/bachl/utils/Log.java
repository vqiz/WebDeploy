/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.utils;

public class Log {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";

    public static void info(String message) {
        System.out.println(BLUE + "[INFO] " + message + RESET);
    }

    public static void warn(String message) {
        System.out.println(YELLOW + "[WARN] " + message + RESET);
    }

    public static void error(String message) {
        System.err.println(RED + "[ERROR] " + message + RESET);
    }
}
