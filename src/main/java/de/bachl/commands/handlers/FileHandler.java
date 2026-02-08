/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.commands.handlers;

import java.util.HashMap;

import com.jcraft.jsch.Session;

import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;

public class FileHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() {
                return "--zip-remote";
            }

            public String getDescription() {
                return "Zip folder (usage: --zip-remote <path>).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String path = args.get("--zip-remote");
                CommandUtils.sendCommand("zip -r " + path + ".zip " + path, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--unzip-remote";
            }

            public String getDescription() {
                return "Unzip file.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String path = args.get("--unzip-remote");
                CommandUtils.sendCommand("unzip " + path, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--mv";
            }

            public String getDescription() {
                return "Move file (usage: --mv 'src dest').";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String p = args.get("--mv"); 
                CommandUtils.sendCommand("mv " + p, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--cp";
            }

            public String getDescription() {
                return "Copy file.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String p = args.get("--cp");
                CommandUtils.sendCommand("cp -r " + p, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--chown";
            }

            public String getDescription() {
                return "Change owner.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String p = args.get("--chown");
                CommandUtils.sendCommand("sudo chown -R " + p, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--chmod";
            }

            public String getDescription() {
                return "Change permissions.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String p = args.get("--chmod");
                CommandUtils.sendCommand("sudo chmod -R " + p, session, true);
            }
        });

    }
}
