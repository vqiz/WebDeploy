/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.utils;

import java.util.HashMap;

public class ArgsConstructor {

    private final String[] args;

    public ArgsConstructor(String[] args) {
        this.args = args;
    }

    public HashMap<String, String> parse() {
        HashMap<String, String> result = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String current = args[i];

            if (current.startsWith("--")) {
                
                String key = current;
                String value = "true"; 

                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[i + 1];
                    i++; 
                }

                if (key.contains("=")) {
                    String[] parts = key.split("=", 2);
                    key = parts[0];
                    value = parts[1];
                }

                result.put(key, value);
            }
        }

        return result;
    }
}
