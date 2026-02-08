/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {

    private final Map<String, Command> commands = new HashMap<>();

    public void register(Command command) {
        commands.put(command.getCommand(), command);
    }

    public Command find(String key) {
        return commands.get(key);
    }

    public void printHelp() {
        System.out.println("WebDeploy - Available Commands:");
        System.out.println("-----------------------------");

        List<String> keys = new ArrayList<>(commands.keySet());
        Collections.sort(keys);

        int maxLen = 0;
        for (String key : keys) {
            maxLen = Math.max(maxLen, key.length());
        }

        for (String key : keys) {
            Command cmd = commands.get(key);
            String desc = cmd.getDescription();
            
            String paddedKey = String.format("%-" + (maxLen + 4) + "s", key);
            System.out.println(paddedKey + desc);
        }
    }
}
