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

        // --- System Ops ---

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

        // --- Services ---

        registry.register(new Command() {
            public String getCommand() {
                return "--service-start";
            }

            public String getDescription() {
                return "Start a service.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-start");
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
                CommandUtils.sendCommand("sudo systemctl restart " + svc, session, true);
            }
        });

        // --- Monitoring / Info ---

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

        // --- Users ---

        registry.register(new Command() {
            public String getCommand() {
                return "--user-add";
            }

            public String getDescription() {
                return "Add a system user.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String user = args.get("--user-add");
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
