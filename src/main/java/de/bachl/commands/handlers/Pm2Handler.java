/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import com.jcraft.jsch.Session;
import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.utils.Log;

import java.util.HashMap;

public class Pm2Handler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() { return "--pm2-install"; }
            public String getDescription() { return "Install PM2 process manager globally."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("npm install -g pm2", session, true);
                Log.info("PM2 installed. Run --pm2-startup to configure auto-start.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-list"; }
            public String getDescription() { return "List all PM2 managed processes."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("pm2 list", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-start"; }
            public String getDescription() { return "Start a process with PM2 (usage: --pm2-start server.js)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String script = args.get("--pm2-start");
                if (script == null || script.equals("true") || script.isEmpty()) {
                    Log.error("Specify a script to start (e.g. --pm2-start server.js).");
                    return;
                }
                CommandUtils.sendCommand("pm2 start " + script, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-stop"; }
            public String getDescription() { return "Stop a PM2 process (usage: --pm2-stop <name|id>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.get("--pm2-stop");
                if (name == null || name.equals("true") || name.isEmpty()) {
                    Log.error("Specify a process name or id (e.g. --pm2-stop my-app).");
                    return;
                }
                CommandUtils.sendCommand("pm2 stop " + name, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-restart"; }
            public String getDescription() { return "Restart a PM2 process (usage: --pm2-restart <name|id>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.get("--pm2-restart");
                if (name == null || name.equals("true") || name.isEmpty()) {
                    Log.error("Specify a process name or id (e.g. --pm2-restart my-app).");
                    return;
                }
                CommandUtils.sendCommand("pm2 restart " + name, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-delete"; }
            public String getDescription() { return "Delete a PM2 process (usage: --pm2-delete <name|id>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.get("--pm2-delete");
                if (name == null || name.equals("true") || name.isEmpty()) {
                    Log.error("Specify a process name or id (e.g. --pm2-delete my-app).");
                    return;
                }
                CommandUtils.sendCommand("pm2 delete " + name, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-logs"; }
            public String getDescription() { return "Show PM2 logs (usage: --pm2-logs <name> or --pm2-logs for all)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.get("--pm2-logs");
                String cmd = (name == null || name.equals("true") || name.isEmpty())
                        ? "pm2 logs --lines 100 --nostream"
                        : "pm2 logs " + name + " --lines 100 --nostream";
                CommandUtils.sendCommand(cmd, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-save"; }
            public String getDescription() { return "Save PM2 process list for auto-restart."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("pm2 save", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-startup"; }
            public String getDescription() { return "Configure PM2 to start on system boot."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("pm2 startup && pm2 save", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-status"; }
            public String getDescription() { return "Show PM2 process details (usage: --pm2-status <name> or all)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.get("--pm2-status");
                if (name == null || name.equals("true") || name.isEmpty()) {
                    CommandUtils.sendCommand("pm2 list", session, true);
                } else {
                    CommandUtils.sendCommand("pm2 show " + name, session, true);
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--pm2-env"; }
            public String getDescription() { return "Show environment variables for a PM2 process (usage: --pm2-env <name>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.get("--pm2-env");
                if (name == null || name.equals("true") || name.isEmpty()) {
                    Log.error("Specify a process name (e.g. --pm2-env my-app).");
                    return;
                }
                CommandUtils.sendCommand("pm2 env " + name, session, true);
            }
        });
    }
}
