package de.bachl.commands.handlers;

import java.util.HashMap;

import com.jcraft.jsch.Session;

import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.utils.Log;

public class DevHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() {
                return "--git-pull";
            }

            public String getDescription() {
                return "Run git pull in project dir.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                CommandUtils.sendCommand("cd /var/www/html/" + config.getProjectname() + " && git pull", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--npm-install";
            }

            public String getDescription() {
                return "Run npm install in project dir.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                CommandUtils.sendCommand("cd /var/www/html/" + config.getProjectname() + " && npm install", session,
                        true);
            }
        });

        // PM2
        registry.register(new Command() {
            public String getCommand() {
                return "--pm2-list";
            }

            public String getDescription() {
                return "List PM2 processes.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("pm2 list", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--pm2-restart";
            }

            public String getDescription() {
                return "Restart PM2 process.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String proc = args.get("--pm2-restart");
                CommandUtils.sendCommand("pm2 restart " + proc, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--docker-ps";
            }

            public String getDescription() {
                return "List docker containers.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo docker ps", session, true);
            }
        });

        // --- PHP ---

        registry.register(new Command() {
            public String getCommand() {
                return "--composer-install";
            }

            public String getDescription() {
                return "Run composer install.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                CommandUtils.sendCommand("cd /var/www/html/" + config.getProjectname()
                        + " && composer install --no-dev --optimize-autoloader", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--composer-update";
            }

            public String getDescription() {
                return "Run composer update.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                CommandUtils.sendCommand("cd /var/www/html/" + config.getProjectname() + " && composer update", session,
                        true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--artisan-cmd";
            }

            public String getDescription() {
                return "Run php artisan cmd (usage: --artisan-cmd 'migrate').";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                String cmd = args.get("--artisan-cmd");
                CommandUtils.sendCommand("cd /var/www/html/" + config.getProjectname() + " && php artisan " + cmd,
                        session, true);
            }
        });

        // --- Python ---

        registry.register(new Command() {
            public String getCommand() {
                return "--pip-install";
            }

            public String getDescription() {
                return "Run pip install -r requirements.txt.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                CommandUtils.sendCommand(
                        "cd /var/www/html/" + config.getProjectname() + " && pip install -r requirements.txt", session,
                        true);
            }
        });

        registry.register(new Command() {
            public String getCommand() {
                return "--venv-create";
            }

            public String getDescription() {
                return "Create python venv.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null)
                    return;
                CommandUtils.sendCommand("cd /var/www/html/" + config.getProjectname() + " && python3 -m venv venv",
                        session, true);
            }
        });

        // --- Misc ---
        registry.register(new Command() {
            public String getCommand() {
                return "--run-script";
            }

            public String getDescription() {
                return "Run executable script.";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String cmd = args.get("--run-script");
                CommandUtils.sendCommand(cmd, session, true);
            }
        });

    }
}
