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
import de.bachl.services.SSLService;
import de.bachl.utils.Log;

public class SecurityHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() {
                return "--firewall-status";
            }

            public String getDescription() {
                return "Check UFW firewall status.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo ufw status", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--firewall-allow";
            }

            public String getDescription() {
                return "Allow port (usage: --firewall-allow <port>).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String port = args.get("--firewall-allow");
                if (port == null || port.equals("true") || port.isEmpty()) {
                    Log.error("Please specify a port to allow (e.g. --firewall-allow 80).");
                    return;
                }
                CommandUtils.sendCommand("sudo ufw allow " + port, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--firewall-deny";
            }

            public String getDescription() {
                return "Deny port (usage: --firewall-deny <port>).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String port = args.get("--firewall-deny");
                if (port == null || port.equals("true") || port.isEmpty()) {
                    Log.error("Please specify a port to deny (e.g. --firewall-deny 80).");
                    return;
                }
                CommandUtils.sendCommand("sudo ufw deny " + port, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--fail2ban-install";
            }

            public String getDescription() {
                return "Install fail2ban.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo apt-get install -y fail2ban", session, true);
                CommandUtils.sendCommand("sudo systemctl start fail2ban", session, true);
                CommandUtils.sendCommand("sudo systemctl enable fail2ban", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--fail2ban-status";
            }

            public String getDescription() {
                return "Check fail2ban status.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo systemctl status fail2ban --no-pager", session, true);
                CommandUtils.sendCommand("sudo fail2ban-client status", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--check-ports";
            }

            public String getDescription() {
                return "List open ports (netstat).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                
                CommandUtils.sendCommand("ss -tuln", session, true);
            }
        });

    }
}
