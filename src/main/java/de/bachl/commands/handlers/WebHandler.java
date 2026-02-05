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

        // --- New Phase 2 --

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
                    // Generic attempt
                    String path = "/var/www/html/" + config.getProjectname();
                    CommandUtils.sendCommand("cd " + path + " && (php artisan optimize:clear || true)", session, true);
                }
            }
        });

    }
}
