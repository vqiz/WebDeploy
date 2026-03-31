/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import com.jcraft.jsch.Session;
import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.utils.Log;

import java.util.HashMap;

public class MonitoringHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() { return "--top"; }
            public String getDescription() { return "Show top processes snapshot."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("top -bn1 | head -30", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--htop"; }
            public String getDescription() { return "Open interactive htop session."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null && !args.containsKey("--server")) {
                    Log.error("Specify a server with --server <name> or run inside a project.");
                    return;
                }
                de.bachl.Config.Config serverCfg = new de.bachl.Config.ConfigProvider().getServerConfig(
                        args.containsKey("--server") ? args.get("--server") : config.getServername());
                CommandUtils.runInteractive(serverCfg.getKeypath(), serverCfg.getUser(), serverCfg.getHost(), "htop");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--disk-io"; }
            public String getDescription() { return "Show disk I/O statistics."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("iostat -x 1 3 2>/dev/null || (echo 'iostat not available, showing df:' && df -h)", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--network-stats"; }
            public String getDescription() { return "Show network statistics."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("ss -s && echo '' && echo '=== Interface Stats ===' && cat /proc/net/dev", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--open-files"; }
            public String getDescription() { return "Show open file descriptor count."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("echo 'Open file descriptors:' && lsof 2>/dev/null | wc -l || cat /proc/sys/fs/file-nr", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--system-temp"; }
            public String getDescription() { return "Show CPU temperature."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand(
                    "cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null | awk '{printf \"CPU Temp: %.1f°C\\n\", $1/1000}' || echo 'Temperature sensor not available'",
                    session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--journal-errors"; }
            public String getDescription() { return "Show recent systemd error logs."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo journalctl -p err -n 50 --no-pager", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--syslog"; }
            public String getDescription() { return "Show last 50 lines of syslog."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo tail -n 50 /var/log/syslog 2>/dev/null || sudo journalctl -n 50 --no-pager", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cpu"; }
            public String getDescription() { return "Show CPU usage."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("top -bn1 | grep 'Cpu(s)' && echo '' && mpstat 2>/dev/null || echo 'Install sysstat for detailed CPU stats'", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--memory"; }
            public String getDescription() { return "Show detailed memory usage."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("free -h && echo '' && cat /proc/meminfo | grep -E 'MemTotal|MemFree|Cached|SwapTotal|SwapFree'", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--processes"; }
            public String getDescription() { return "Show top 20 processes by CPU."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("ps aux --sort=-%cpu | head -21", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--load"; }
            public String getDescription() { return "Show system load average."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("uptime && cat /proc/loadavg", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--connections"; }
            public String getDescription() { return "Show active network connections."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("ss -tuln && echo '' && echo '=== Established ===' && ss -tun", session, true);
            }
        });
    }
}
