/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

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
            public String getCommand() { return "--reboot"; }
            public String getDescription() { return "Reboot the remote server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo reboot", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--shutdown"; }
            public String getDescription() { return "Shutdown the remote server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo shutdown now", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--update-server"; }
            public String getDescription() { return "Run apt-get update & upgrade."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo apt-get update && sudo apt-get upgrade -y", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--install-pkg"; }
            public String getDescription() { return "Install a package (usage: --install-pkg <name>)."; }
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
            public String getCommand() { return "--service-start"; }
            public String getDescription() { return "Start a systemd service."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-start");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service (e.g. --service-start nginx).");
                    return;
                }
                CommandUtils.sendCommand("sudo systemctl start " + svc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--service-stop"; }
            public String getDescription() { return "Stop a systemd service."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-stop");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service (e.g. --service-stop nginx).");
                    return;
                }
                CommandUtils.sendCommand("sudo systemctl stop " + svc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--service-restart"; }
            public String getDescription() { return "Restart a systemd service."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-restart");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service (e.g. --service-restart nginx).");
                    return;
                }
                CommandUtils.sendCommand("sudo systemctl restart " + svc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--service-status"; }
            public String getDescription() { return "Show status of a service (usage: --service-status <name>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-status");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service name.");
                    return;
                }
                CommandUtils.sendCommand("systemctl status " + svc + " --no-pager", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--service-enable"; }
            public String getDescription() { return "Enable a service on boot (usage: --service-enable <name>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-enable");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service name.");
                    return;
                }
                CommandUtils.sendCommand("sudo systemctl enable " + svc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--service-disable"; }
            public String getDescription() { return "Disable a service on boot (usage: --service-disable <name>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String svc = args.get("--service-disable");
                if (svc == null || svc.equals("true") || svc.isEmpty()) {
                    Log.error("Please specify a service name.");
                    return;
                }
                CommandUtils.sendCommand("sudo systemctl disable " + svc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--service-list"; }
            public String getDescription() { return "List active systemd services."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("systemctl list-units --type=service --state=active --no-pager | head -40", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--uptime"; }
            public String getDescription() { return "Show server uptime."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("uptime", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--kernel"; }
            public String getDescription() { return "Show kernel version."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("uname -r", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--info"; }
            public String getDescription() { return "Show system info (CPU, RAM, disk)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("lscpu | grep 'Model name'", session, true);
                CommandUtils.sendCommand("free -h", session, true);
                CommandUtils.sendCommand("df -h /", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--network"; }
            public String getDescription() { return "Show network interfaces."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("ip a", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--whoami"; }
            public String getDescription() { return "Show current remote user."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("whoami", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--disk-cleanup"; }
            public String getDescription() { return "Clean up disk space (autoremove, journal vacuum)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo apt-get autoremove -y && sudo apt-get autoclean", session, true);
                CommandUtils.sendCommand("sudo journalctl --vacuum-time=3d", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--diskspace"; }
            public String getDescription() { return "Show available disk space."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("df -h", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--user-add"; }
            public String getDescription() { return "Add a system user (usage: --user-add <name>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String user = args.get("--user-add");
                if (user == null || user.equals("true") || user.isEmpty()) {
                    Log.error("Please specify a username (e.g. --user-add john).");
                    return;
                }
                CommandUtils.sendCommand("sudo useradd -m " + user, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--user-remove"; }
            public String getDescription() { return "Remove a system user (usage: --user-remove <name>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String user = args.get("--user-remove");
                if (user == null || user.equals("true") || user.isEmpty()) {
                    Log.error("Please specify a username (e.g. --user-remove john).");
                    return;
                }
                CommandUtils.sendCommand("sudo userdel -r " + user, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--test-connection"; }
            public String getDescription() { return "Verify SSH connectivity to the server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("echo 'Connection Successful'", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cpu"; }
            public String getDescription() { return "Show CPU usage."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("top -bn1 | grep \"Cpu(s)\"", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--memory"; }
            public String getDescription() { return "Show memory usage."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("free -h && echo '' && cat /proc/meminfo | grep -E 'MemTotal|MemFree|Cached'", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--processes"; }
            public String getDescription() { return "Show top processes by CPU usage."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("ps aux --sort=-%cpu | head -20", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--load"; }
            public String getDescription() { return "Show system load average."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("cat /proc/loadavg && uptime", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--ping"; }
            public String getDescription() { return "Ping a host from server (usage: --ping <host>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String host = args.get("--ping");
                if (host == null || host.equals("true") || host.isEmpty()) {
                    Log.error("Please specify a host to ping.");
                    return;
                }
                CommandUtils.sendCommand("ping -c 4 " + host, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--hostname"; }
            public String getDescription() { return "Show server hostname and IP addresses."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("hostname && hostname -I", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--timezone"; }
            public String getDescription() { return "Show or set timezone (usage: --timezone show|<tz>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String tz = args.get("--timezone");
                if (tz == null || tz.equals("true") || tz.equalsIgnoreCase("show")) {
                    CommandUtils.sendCommand("timedatectl", session, true);
                } else {
                    CommandUtils.sendCommand("sudo timedatectl set-timezone " + tz, session, true);
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--os-info"; }
            public String getDescription() { return "Show OS release and kernel information."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("cat /etc/os-release && uname -a", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--env-vars"; }
            public String getDescription() { return "List all environment variables on server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("printenv | sort", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--backupserver"; }
            public String getDescription() { return "Download a full server backup to a local path (usage: --backupserver <local-destination-path>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String localDest = args.get("--backupserver");
                if (localDest == null || localDest.equals("true") || localDest.isEmpty()) {
                    Log.error("Please specify a local destination path (e.g. --backupserver /backups/myserver).");
                    return;
                }

                String timestamp = String.valueOf(System.currentTimeMillis());
                String localFile = localDest.endsWith(".tar.gz") ? localDest : localDest + "/server_backup_" + timestamp + ".tar.gz";

                // Ensure local destination directory exists
                java.io.File localDir = new java.io.File(localDest.endsWith(".tar.gz")
                        ? new java.io.File(localDest).getParent()
                        : localDest);
                if (!localDir.exists() && !localDir.mkdirs()) {
                    Log.error("Could not create local destination directory: " + localDir.getAbsolutePath());
                    return;
                }

                Log.info("Streaming full server backup to " + localFile + " (no remote disk space used)...");
                com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
                channel.setCommand("sudo tar -czf - --ignore-failed-read /etc /var /home /root 2>/dev/null");
                channel.connect();

                try (java.io.InputStream in = channel.getInputStream();
                     java.io.FileOutputStream out = new java.io.FileOutputStream(localFile)) {
                    byte[] buf = new byte[8192];
                    int n;
                    long total = 0;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        total += n;
                        if (total % (1024 * 1024 * 10) < 8192) {
                            Log.info("  " + (total / 1024 / 1024) + " MB received...");
                        }
                    }
                }
                channel.disconnect();
                Log.success("Server backup saved to: " + localFile);
            }
        });
    }
}
