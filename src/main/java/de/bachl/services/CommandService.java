package de.bachl.services;

import java.util.HashMap;

import com.jcraft.jsch.Session;

import de.bachl.Config.Config;
import de.bachl.Config.ConfigProvider;
import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.commands.handlers.*;
import de.bachl.setup.DeployService;
import de.bachl.utils.Log;

public class CommandService {

    private final HashMap<String, String> args;
    private final CommandRegistry registry;
    private Config config;
    private ProjectConfig projectConfig;

    public CommandService(HashMap<String, String> args) {
        this.args = args;
        this.registry = new CommandRegistry();
        initializeRegistry();
        loadConfig();
    }

    private void initializeRegistry() {
        new SystemHandler().register(registry);
        new SecurityHandler().register(registry);
        new WebHandler().register(registry);
        new DevHandler().register(registry);
        new FileHandler().register(registry);
        new ProjectHandler().register(registry);
    }

    private void loadConfig() {
        try {
            ConfigProvider cp = new ConfigProvider();
            this.projectConfig = cp.getProjectConfig();
            if (this.projectConfig != null) {
                this.config = cp.getServerConfig(this.projectConfig.getServername());
            }
        } catch (Exception e) {
        }
    }

    private Session getSession() throws Exception {
        if (config == null) {
            Log.error("No project configuration found. Run command inside a WebDeploy project directory.");
            System.exit(1);
        }
        return new DeployService().connectSSH(config);
    }

    public void handle() {
        try {
            // Priority Local Commands
            if (args.containsKey("--help")) {
                registry.printHelp();
                return;
            }
            if (args.containsKey("--version")) {
                System.out.println("WebDeploy v1.2.0 - Mega Upgrade");
                return;
            }
            if (args.containsKey("--ssh")) {
                handleInteractive("");
                return;
            }
            if (args.containsKey("--monitor")) {
                handleInteractive("htop");
                return;
            }
            if (args.containsKey("--pm2-logs")) {
                String proc = args.get("--pm2-logs");
                handleInteractive("pm2 logs " + (proc.equals("true") ? "" : proc));
                return;
            }

            // Find matching command
            Command found = null;
            for (String key : args.keySet()) {
                found = registry.find(key);
                if (found != null)
                    break;
            }

            if (found != null) {
                Session session = getSession();
                try {
                    found.execute(session, args, projectConfig);
                } finally {
                    session.disconnect();
                }
            } else {
                if (args.containsKey("--list-projects"))
                    Log.info("Use --listservers to see servers.");
                else
                    Log.warn("Unknown command. Use --help.");
            }

        } catch (Exception e) {
            Log.error("Command Execution Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleInteractive(String cmd) throws Exception {
        if (config == null) {
            Log.error("Project config missing for SSH.");
            return;
        }
        CommandUtils.runInteractive(config.getKeypath(), config.getUser(), config.getHost(), cmd);
    }

}
