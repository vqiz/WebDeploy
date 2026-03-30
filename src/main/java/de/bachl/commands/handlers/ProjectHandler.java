/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import java.util.HashMap;

import com.jcraft.jsch.Session;

import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.utils.Log;

public class ProjectHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() { return "--backup"; }
            public String getDescription() { return "Backup remote project to local archive."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String remotePath = "/var/www/html/" + (config != null ? config.getProjectname() : "");
                String localDest = "backup_" + System.currentTimeMillis();
                Log.info("Backing up " + remotePath + " to ./" + localDest);
                String tarFile = "/tmp/backup_" + System.currentTimeMillis() + ".tar.gz";
                CommandUtils.sendCommand("tar -czf " + tarFile + " " + remotePath, session, true);
                com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                channelSftp.get(tarFile, localDest + ".tar.gz");
                channelSftp.disconnect();
                CommandUtils.sendCommand("rm " + tarFile, session, false);
                Log.success("Backup downloaded: " + localDest + ".tar.gz");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--delete-project"; }
            public String getDescription() { return "Delete project from server (files + Nginx config)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Not in a project directory.");
                    return;
                }
                Log.warn("Deleting project " + config.getProjectname() + " from server...");
                CommandUtils.sendCommand("sudo rm -rf /var/www/html/" + config.getProjectname(), session, true);
                CommandUtils.sendCommand("sudo rm -f /etc/nginx/sites-enabled/" + config.getProjectname(), session, true);
                CommandUtils.sendCommand("sudo rm -f /etc/nginx/sites-available/" + config.getProjectname(), session, true);
                CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
                Log.success("Project deleted.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--rollback"; }
            public String getDescription() { return "Rollback to previous deployment release."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String projectName = config.getProjectname();
                String storageBase = "/var/www/webdeploy/" + projectName;
                String releasesPath = storageBase + "/releases";

                // Use ls -1t for time-based sort (newest first)
                String output;
                try {
                    output = CommandUtils.runCommandWithOutput("ls -1t " + releasesPath, session);
                } catch (Exception e) {
                    Log.error("Failed to list releases: " + e.getMessage());
                    return;
                }

                if (output == null || output.isEmpty()) {
                    Log.error("No releases found.");
                    return;
                }
                String[] releases = output.split("\n");

                String currentLink = CommandUtils.runCommandWithOutput("readlink " + storageBase + "/current", session);
                String currentRelease = "";
                if (currentLink != null && currentLink.contains("/")) {
                    currentRelease = currentLink.substring(currentLink.lastIndexOf("/") + 1).trim();
                } else if (currentLink != null) {
                    currentRelease = currentLink.trim();
                }

                // releases[0] is newest; find current, then previous = releases[i+1]
                String previousRelease = null;
                for (int i = 0; i < releases.length; i++) {
                    if (releases[i].trim().equals(currentRelease)) {
                        if (i + 1 < releases.length) {
                            previousRelease = releases[i + 1].trim();
                        }
                        break;
                    }
                }

                if (previousRelease != null && !previousRelease.isEmpty()) {
                    Log.info("Rolling back from " + currentRelease + " to " + previousRelease);
                    CommandUtils.sendCommand(
                            "ln -sfn " + releasesPath + "/" + previousRelease + " " + storageBase + "/current",
                            session, true);
                    CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
                    Log.success("Rollback successful.");
                } else {
                    Log.warn("No previous release found to rollback to (current: " + currentRelease + ").");
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--audit"; }
            public String getDescription() { return "Show release history and current deployment."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String projectName = config.getProjectname();
                String storageBase = "/var/www/webdeploy/" + projectName;

                Log.info("Release history for project: " + projectName);
                CommandUtils.sendCommand("ls -la " + storageBase + "/releases/", session, true);

                String currentLink = CommandUtils.runCommandWithOutput("readlink " + storageBase + "/current", session);
                if (currentLink != null && !currentLink.isEmpty()) {
                    String currentRelease = currentLink.substring(currentLink.lastIndexOf("/") + 1).trim();
                    Log.info("Current release: " + currentRelease + "  <-- ACTIVE");
                } else {
                    Log.warn("No current symlink set.");
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--releases"; }
            public String getDescription() { return "List all releases with current marked."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String storageBase = "/var/www/webdeploy/" + config.getProjectname();
                String output = CommandUtils.runCommandWithOutput("ls -lt " + storageBase + "/releases/", session);
                String currentLink = CommandUtils.runCommandWithOutput("readlink " + storageBase + "/current", session);
                String currentRelease = "";
                if (currentLink != null && currentLink.contains("/")) {
                    currentRelease = currentLink.substring(currentLink.lastIndexOf("/") + 1).trim();
                }

                for (String line : output.split("\n")) {
                    if (!currentRelease.isEmpty() && line.contains(currentRelease)) {
                        System.out.println(line + "  <-- current");
                    } else {
                        System.out.println(line);
                    }
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--deploy-status"; }
            public String getDescription() { return "Show current symlink target and its creation date."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String storageBase = "/var/www/webdeploy/" + config.getProjectname();
                String currentLink = CommandUtils.runCommandWithOutput("readlink " + storageBase + "/current", session);
                if (currentLink == null || currentLink.isEmpty()) {
                    Log.warn("No current deployment symlink found.");
                    return;
                }
                Log.info("Current deployment: " + currentLink);
                CommandUtils.sendCommand("stat " + currentLink + " | grep -E 'File:|Modify:'", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--health-check"; }
            public String getDescription() { return "Check if deployed site is responding."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String url = null;
                if (config != null && config.getHealthCheckUrl() != null && !config.getHealthCheckUrl().isEmpty()) {
                    url = config.getHealthCheckUrl();
                } else if (config != null && config.getDomain() != null && !config.getDomain().isEmpty()) {
                    url = "http://" + config.getDomain() + "/";
                }
                if (url == null) {
                    Log.error("No health check URL or domain configured.");
                    return;
                }
                Log.info("Checking: " + url);
                CommandUtils.sendCommand("curl -sSf " + url + " --max-time 10 && echo 'Health check passed' || echo 'Health check FAILED'", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cleanup-releases"; }
            public String getDescription() { return "Remove old releases, keeping the last N (default 5)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                int keep = config.getKeepReleases();
                String storageBase = "/var/www/webdeploy/" + config.getProjectname();
                String cleanupCmd = "cd " + storageBase + "/releases && ls -1t | tail -n +" + (keep + 1) + " | xargs -I {} rm -rf {}";
                CommandUtils.sendCommand(cleanupCmd, session, true);
                Log.success("Cleanup done. Kept last " + keep + " releases.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--db-backup"; }
            public String getDescription() { return "Backup database (usage: --db-backup --name=<db> --type=<mysql|pgsql>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.getOrDefault("--name", "db");
                String type = args.getOrDefault("--type", "mysql");
                String dumpCmd = (type.equals("pgsql") ? "pg_dump " : "mysqldump ") + name + " > /tmp/" + name + ".sql";
                CommandUtils.sendCommand(dumpCmd, session, true);
                Log.success("Database dumped to /tmp/" + name + ".sql on remote.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--db-restore"; }
            public String getDescription() { return "Restore database (usage: --db-restore type:dbname:/path/to/file)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String value = args.get("--db-restore");
                if (value == null || value.equals("true") || value.isEmpty()) {
                    Log.error("Usage: --db-restore mysql:mydb:/tmp/mydb.sql");
                    return;
                }
                String[] parts = value.split(":", 3);
                if (parts.length < 3) {
                    Log.error("Usage: --db-restore type:dbname:/path/to/file (e.g. mysql:mydb:/tmp/mydb.sql)");
                    return;
                }
                String type = parts[0].trim();
                String dbName = parts[1].trim();
                String remoteFile = parts[2].trim();

                String restoreCmd;
                switch (type.toLowerCase()) {
                    case "pgsql":
                    case "postgres":
                        restoreCmd = "sudo -u postgres psql " + dbName + " < " + remoteFile;
                        break;
                    case "mysql":
                    default:
                        restoreCmd = "mysql " + dbName + " < " + remoteFile;
                        break;
                }
                Log.info("Restoring " + dbName + " from " + remoteFile + " using " + type + "...");
                CommandUtils.sendCommand(restoreCmd, session, true);
                Log.success("Database restore complete.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--env-set"; }
            public String getDescription() { return "Append an env var to .env (usage: --env-set 'KEY=VAL')."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) return;
                String key = args.get("--env-set");
                String remotePath = "/var/www/html/" + config.getProjectname() + "/.env";
                CommandUtils.sendCommand("echo '" + key + "' | sudo tee -a " + remotePath, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--env-get"; }
            public String getDescription() { return "Print .env file contents."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) return;
                String remotePath = "/var/www/html/" + config.getProjectname() + "/.env";
                CommandUtils.sendCommand("cat " + remotePath, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--env-list"; }
            public String getDescription() { return "List .env file contents."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) return;
                String remotePath = "/var/www/html/" + config.getProjectname() + "/.env";
                CommandUtils.sendCommand("cat " + remotePath, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cron-list"; }
            public String getDescription() { return "List cron jobs on the server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("crontab -l", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cron-add"; }
            public String getDescription() { return "Add a cron job (usage: --cron-add '*/5 * * * * /path/script')."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String cron = args.get("--cron-add");
                if (cron == null || cron.equals("true") || cron.isEmpty()) {
                    Log.error("Usage: --cron-add '*/5 * * * * /path/to/script'");
                    return;
                }
                String addCmd = "(crontab -l 2>/dev/null; echo \"" + cron + "\") | crontab -";
                CommandUtils.sendCommand(addCmd, session, true);
                Log.success("Cron job added: " + cron);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cron-remove"; }
            public String getDescription() { return "Remove cron jobs matching pattern (usage: --cron-remove <pattern>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String pattern = args.get("--cron-remove");
                if (pattern == null || pattern.equals("true") || pattern.isEmpty()) {
                    Log.error("Usage: --cron-remove <pattern>");
                    return;
                }
                String removeCmd = "crontab -l | grep -v \"" + pattern + "\" | crontab -";
                CommandUtils.sendCommand(removeCmd, session, true);
                Log.success("Cron jobs matching '" + pattern + "' removed.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--dig"; }
            public String getDescription() { return "DNS lookup for project domain."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config != null && config.isEnabledomain()) {
                    CommandUtils.sendCommand("dig " + config.getDomain(), session, true);
                } else {
                    Log.info("No domain configured to dig.");
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--whois"; }
            public String getDescription() { return "Whois lookup for project domain."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config != null && config.isEnabledomain()) {
                    CommandUtils.sendCommand("whois " + config.getDomain(), session, true);
                }
            }
        });
    }
}
