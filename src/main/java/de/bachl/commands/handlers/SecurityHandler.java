/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

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
            public String getCommand() { return "--firewall-status"; }
            public String getDescription() { return "Check UFW firewall status."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo ufw status", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--firewall-allow"; }
            public String getDescription() { return "Allow a port (usage: --firewall-allow <port>)."; }
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
            public String getCommand() { return "--firewall-deny"; }
            public String getDescription() { return "Deny a port (usage: --firewall-deny <port>)."; }
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
            public String getCommand() { return "--firewall-enable"; }
            public String getDescription() { return "Enable UFW firewall."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo ufw --force enable", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--firewall-disable"; }
            public String getDescription() { return "Disable UFW firewall."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo ufw disable", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--firewall-list"; }
            public String getDescription() { return "List UFW rules (numbered)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo ufw status numbered", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--firewall-reset"; }
            public String getDescription() { return "Reset UFW to defaults."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo ufw --force reset", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--setup-ufw-defaults"; }
            public String getDescription() { return "Enable UFW with secure defaults (allow 22,80,443; deny all else)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo ufw default deny incoming", session, true);
                CommandUtils.sendCommand("sudo ufw default allow outgoing", session, true);
                CommandUtils.sendCommand("sudo ufw allow 22", session, true);
                CommandUtils.sendCommand("sudo ufw allow 80", session, true);
                CommandUtils.sendCommand("sudo ufw allow 443", session, true);
                CommandUtils.sendCommand("sudo ufw --force enable", session, true);
                Log.success("UFW configured with secure defaults.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--fail2ban-install"; }
            public String getDescription() { return "Install and enable fail2ban."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo apt-get install -y fail2ban", session, true);
                CommandUtils.sendCommand("sudo systemctl start fail2ban", session, true);
                CommandUtils.sendCommand("sudo systemctl enable fail2ban", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--fail2ban-status"; }
            public String getDescription() { return "Check fail2ban status."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo systemctl status fail2ban --no-pager", session, true);
                CommandUtils.sendCommand("sudo fail2ban-client status", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--fail2ban-unban"; }
            public String getDescription() { return "Unban an IP from fail2ban (usage: --fail2ban-unban <ip>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String ip = args.get("--fail2ban-unban");
                if (ip == null || ip.equals("true") || ip.isEmpty()) {
                    Log.error("Please specify an IP address to unban.");
                    return;
                }
                CommandUtils.sendCommand("sudo fail2ban-client unban " + ip, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--check-ports"; }
            public String getDescription() { return "List open ports."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("ss -tuln", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--ssl-renew"; }
            public String getDescription() { return "Renew SSL certificates via certbot."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo certbot renew --nginx", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--ssl-status"; }
            public String getDescription() { return "Check SSL certificate dates (usage: --ssl-status <domain>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String domain = args.get("--ssl-status");
                if (domain == null || domain.equals("true") || domain.isEmpty()) {
                    if (config != null && config.getDomain() != null) {
                        domain = config.getDomain();
                    } else {
                        Log.error("Please specify a domain (e.g. --ssl-status example.com).");
                        return;
                    }
                }
                CommandUtils.sendCommand("echo | openssl s_client -servername " + domain + " -connect " + domain
                        + ":443 2>/dev/null | openssl x509 -noout -dates", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--last-logins"; }
            public String getDescription() { return "Show last 20 logins."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("last -n 20", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--auth-log"; }
            public String getDescription() { return "Show last 100 lines of auth.log."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo tail -n 100 /var/log/auth.log", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--ssh-config"; }
            public String getDescription() { return "Show active SSH daemon configuration."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("cat /etc/ssh/sshd_config | grep -v '^#' | grep -v '^$'", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--ssl-setup"; }
            public String getDescription() { return "Install certbot and setup SSL for project domain."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null || config.getDomain() == null || config.getDomain().isEmpty()) {
                    Log.error("Project config with domain required.");
                    return;
                }
                SSLService ssl = new SSLService();
                ssl.installCertbot(session);
                ssl.setupSSL(session, config.getDomain());
            }
        });
    }
}
