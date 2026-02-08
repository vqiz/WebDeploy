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
import de.bachl.utils.Log;

public class WebHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() {
                return "--restart";
            }

            public String getDescription() {
                return "Restart Nginx.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo systemctl restart nginx", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--status";
            }

            public String getDescription() {
                return "Nginx Status.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("systemctl status nginx --no-pager", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--logs";
            }

            public String getDescription() {
                return "Tails error.log.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("tail -n 50 /var/log/nginx/error.log", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--access-logs";
            }

            public String getDescription() {
                return "Tails access.log.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("tail -n 50 /var/log/nginx/access.log", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--journal-failed";
            }

            public String getDescription() {
                return "Show failed systemd units.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("journalctl -p 3 -xb", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--clear-cache";
            }

            public String getDescription() {
                return "Clear caches (Nginx + Project).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
                if (config != null) {

                    String path = "/var/www/html/" + config.getProjectname();
                    CommandUtils.sendCommand("cd " + path + " && (php artisan optimize:clear || true)", session, true);
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--maintenance";
            }

            public String getDescription() {
                return "Toggle maintenance mode page.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String path = "/var/www/html/" + config.getProjectname() + "/maintenance.html";

                boolean exists = false;
                try {
                    CommandUtils.sendCommand("ls " + path, session, false);
                    exists = true;
                } catch (Exception e) {
                    exists = false;
                }

                if (exists) {
                    Log.info("Disabling maintenance mode...");
                    CommandUtils.sendCommand("rm " + path, session, true);
                } else {
                    Log.info("Enabling maintenance mode...");
                    CommandUtils.sendCommand("echo '<h1>Maintenance Mode</h1>' > " + path, session, true);
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--max-body-size";
            }

            public String getDescription() {
                return "Set Nginx client_max_body_size (usage: --max-body-size 500M).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String size = args.get("--max-body-size");
                if (size == null || size.equals("true") || size.isEmpty()) {
                    Log.error("Please specify size (e.g. 500M).");
                    return;
                }

                config.setClientMaxBodySize(size);
                new de.bachl.Config.ConfigProvider().setupProject(config);
                Log.info("Updated project config with client_max_body_size: " + size);

                Log.info("Applying Nginx configuration...");
                new de.bachl.services.NginxService().setupSite(session, config);
                Log.info("Nginx configuration updated.");
            }
        });
    }
}
