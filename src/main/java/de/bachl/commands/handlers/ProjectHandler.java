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

        // --- Files / Project Ops ---

        registry.register(new Command() {
            public String getCommand() {
                return "--backup";
            }

            public String getDescription() {
                return "Backup remote project to local.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                // Simplified migration of logic
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
                Log.info("Backup downloaded: " + localDest + ".tar.gz");
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--delete-project";
            }

            public String getDescription() {
                return "Start a service.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Not in a project.");
                    return;
                }
                Log.warn("Deleting project " + config.getProjectname() + " from server...");
                CommandUtils.sendCommand("sudo rm -rf /var/www/html/" + config.getProjectname(), session, true);
                CommandUtils.sendCommand("sudo rm -f /etc/nginx/sites-enabled/" + config.getProjectname(), session,
                        true);
                CommandUtils.sendCommand("sudo rm -f /etc/nginx/sites-available/" + config.getProjectname(), session,
                        true);
                CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
                Log.info("Project deleted.");
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--rollback";
            }

            public String getDescription() {
                return "Rollback deployment.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Rollback requires a versioned deployment strategy which is not yet enabled.");
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--audit";
            }

            public String getDescription() {
                return "Show deployment history.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Audit log placeholder.");
            }
        });

        // --- Database ---

        registry.register(new Command() {
            public String getCommand() {
                return "--db-backup";
            }

            public String getDescription() {
                return "Backup database (usage: --db-backup --name=<db> --type=<mysql|pgsql>).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String name = args.getOrDefault("name", "db");
                String type = args.getOrDefault("type", "mysql");
                String dumpCmd = (type.equals("pgsql") ? "pg_dump " : "mysqldump ") + name + " > /tmp/" + name + ".sql";
                CommandUtils.sendCommand(dumpCmd, session, true);
                Log.info("Database dumped to /tmp/" + name + ".sql on remote.");
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--db-restore";
            }

            public String getDescription() {
                return "Restore database (placeholder).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Use local tools to upload and restore.");
            }
        });

        // --- Env ---

        registry.register(new Command() {
            public String getCommand() {
                return "--env-set";
            }

            public String getDescription() {
                return "Set env var (usage: --env-set 'KEY=VAL').";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                String key = args.get("--env-set");
                String remotePath = "/var/www/html/" + config.getProjectname() + "/.env";
                CommandUtils.sendCommand("echo '" + key + "' | sudo tee -a " + remotePath, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--env-get";
            }

            public String getDescription() {
                return "Get env vars.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                String remotePath = "/var/www/html/" + config.getProjectname() + "/.env";
                CommandUtils.sendCommand("cat " + remotePath, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--env-list";
            }

            public String getDescription() {
                return "List env vars.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                String remotePath = "/var/www/html/" + config.getProjectname() + "/.env";
                CommandUtils.sendCommand("cat " + remotePath, session, true);
            }
        });

        // --- Cron ---

        registry.register(new Command() {
            public String getCommand() {
                return "--cron-list";
            }

            public String getDescription() {
                return "List cron jobs.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("crontab -l", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--cron-add";
            }

            public String getDescription() {
                return "Add cron job.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Use SSH to edit crontab.");
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--cron-remove";
            }

            public String getDescription() {
                return "Remove cron job.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Use SSH to edit crontab.");
            }
        });

        // --- Other System Wrappers that need project config or context ---

        registry.register(new Command() {
            public String getCommand() {
                return "--dig";
            }

            public String getDescription() {
                return "DNS lookup.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config != null && config.isEnabledomain()) {
                    CommandUtils.sendCommand("dig " + config.getDomain(), session, true);
                } else {
                    Log.info("No domain configured to dig.");
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--whois";
            }

            public String getDescription() {
                return "Whois lookup.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config != null && config.isEnabledomain()) {
                    CommandUtils.sendCommand("whois " + config.getDomain(), session, true);
                }
            }
        });

    }
}
