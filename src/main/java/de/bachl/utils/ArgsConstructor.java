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
                // It's a flag/key
                String key = current;
                String value = "true"; // Default for boolean flags

                // Check if next arg is a value (not starting with --)
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[i + 1];
                    i++; // Skip next arg since we consumed it as value
                }

                // Handle --key=value style if necessary, but for now assuming space separation
                // or simple flags
                // If user passes --host=1.2.3.4, split it
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
