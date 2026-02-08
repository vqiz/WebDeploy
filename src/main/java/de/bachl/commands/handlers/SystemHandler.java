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

public class SystemHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() {
                return "--reboot";
            }

            public String getDescription() {
                return "Reboot the remote server.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo reboot", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--shutdown";
            }

            public String getDescription() {
                return "Shutdown the remote server.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo shutdown now", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--update-server";
            }

            public String getDescription() {
                return "Run apt-get update & upgrade.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo apt-get update && sudo apt-get upgrade -y", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--install-pkg";
            }

            public String getDescription() {
                return "Install a package (usage: --install-pkg <name>).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String pkg = args.get("--install-pkg");
                if (pkg == null || pkg.equals("true")) {
                    Log.error("Specify package name");
                    return;
                }
                CommandUtils.sendCommand("sudo apt-get install -y " + pkg, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--service-start";
            }

            public String getDescription() {
                return "Start a service.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-start");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service to start (e.g. --service-start nginx).");
                    return;
                }
                CommandUtils.sendCommand("sudo systemctl start " + svc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--service-stop";
            }

            public String getDescription() {
                return "Stop a service.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-stop");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service to stop (e.g. --service-stop nginx).");
                    return;
                }
                CommandUtils.sendCommand("sudo systemctl stop " + svc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--service-restart";
            }

            public String getDescription() {
                return "Restart a service.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-restart");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service to restart (e.g. --service-restart nginx).");
                    return;
                }
                CommandUtils.sendCommand("sudo systemctl restart " + svc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--uptime";
            }

            public String getDescription() {
                return "Show server uptime.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("uptime", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--kernel";
            }

            public String getDescription() {
                return "Show kernel version.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("uname -r", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--info";
            }

            public String getDescription() {
                return "Show system info.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("lscpu | grep 'Model name'", session, true);
                CommandUtils.sendCommand("free -h", session, true);
                CommandUtils.sendCommand("df -h /", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--network";
            }

            public String getDescription() {
                return "Show network info.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("ip a", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--whoami";
            }

            public String getDescription() {
                return "Show current user.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("whoami", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--disk-cleanup";
            }

            public String getDescription() {
                return "Clean up disk space.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo apt-get autoremove -y && sudo apt-get autoclean", session, true);
                CommandUtils.sendCommand("sudo journalctl --vacuum-time=3d", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--diskspace";
            }

            public String getDescription() {
                return "Show available disk space.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("df -h", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--user-add";
            }

            public String getDescription() {
                return "Add a system user.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String user = args.get("--user-add");
                if (user == null || user.equals("true") || user.isEmpty()) {
                    Log.error("Please specify a username to add (e.g. --user-add john).");
                    return;
                }
                CommandUtils.sendCommand("sudo useradd -m " + user, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--user-remove";
            }

            public String getDescription() {
                return "Remove a system user.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String user = args.get("--user-remove");
                if (user == null || user.equals("true") || user.isEmpty()) {
                    Log.error("Please specify a username to remove (e.g. --user-remove john).");
                    return;
                }
                CommandUtils.sendCommand("sudo userdel -r " + user, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--test-connection";
            }

            public String getDescription() {
                return "Verify SSH connectivity to the server.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("echo 'Connection Successful'", session, true);
            }
        });

    }

}
