/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jcraft.jsch.Session;
import de.bachl.Config.Config;
import de.bachl.Config.ConfigProvider;
import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.utils.Log;

import java.io.File;
import java.util.HashMap;

public class ConfigHandler {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() { return "--show-config"; }
            public String getDescription() { return "Show current project config (webdeploy.config)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("No webdeploy.config found in this directory. Run --setupproject first.");
                    return;
                }
                System.out.println(gson.toJson(config));
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--show-server"; }
            public String getDescription() { return "Show server config (usage: --show-server <name>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig projectConfig) throws Exception {
                String serverName = args.get("--show-server");
                if (serverName == null || serverName.equals("true") || serverName.isEmpty()) {
                    if (projectConfig != null) {
                        serverName = projectConfig.getServername();
                    } else {
                        Log.error("Specify a server name: --show-server <name>");
                        return;
                    }
                }
                Config config = new ConfigProvider().getServerConfig(serverName);
                // Mask the password
                Config masked = new Config(config.getKeypath(), config.getName(), config.getHost(),
                        config.getUser(), "***masked***");
                masked.setSshPort(config.getSshPort());
                System.out.println(gson.toJson(masked));
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--remove-server"; }
            public String getDescription() { return "Remove a server config (usage: --remove-server <name>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.get("--remove-server");
                if (name == null || name.equals("true") || name.isEmpty()) {
                    Log.error("Specify a server name: --remove-server <name>");
                    return;
                }
                File serverFile = new File(System.getProperty("user.home") + "/.webdeploy/servers/" + name);
                if (!serverFile.exists()) {
                    Log.error("Server config not found: " + name);
                    return;
                }
                if (serverFile.delete()) {
                    Log.info("Server config removed: " + name);
                } else {
                    Log.error("Failed to remove server config: " + name);
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--edit-server"; }
            public String getDescription() { return "Update server field (usage: --edit-server name --host=<ip>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig projectConfig) throws Exception {
                String serverName = args.get("--edit-server");
                if (serverName == null || serverName.equals("true") || serverName.isEmpty()) {
                    Log.error("Specify a server name: --edit-server <name>");
                    return;
                }
                ConfigProvider cp = new ConfigProvider();
                Config config = cp.getServerConfig(serverName);
                boolean changed = false;

                if (args.containsKey("--host")) {
                    config.setHost(args.get("--host"));
                    Log.info("Updated host to: " + args.get("--host"));
                    changed = true;
                }
                if (args.containsKey("--user")) {
                    config.setUser(args.get("--user"));
                    Log.info("Updated user to: " + args.get("--user"));
                    changed = true;
                }
                if (args.containsKey("--sshport")) {
                    try {
                        config.setSshPort(Integer.parseInt(args.get("--sshport")));
                        Log.info("Updated SSH port to: " + args.get("--sshport"));
                        changed = true;
                    } catch (NumberFormatException e) {
                        Log.error("Invalid port number: " + args.get("--sshport"));
                    }
                }

                if (changed) {
                    cp.setupServer(config);
                    Log.info("Server config updated: " + serverName);
                } else {
                    Log.warn("No changes specified. Use --host, --user, or --sshport.");
                }
            }
        });
    }
}
