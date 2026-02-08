/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

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

        new FileHandler().register(registry);
        new ProjectHandler().register(registry);
        new de.bachl.commands.handlers.DomainHandler().register(registry);
        new de.bachl.commands.handlers.SFTPHandler().register(registry);
    }

    private void loadConfig() {
        try {
            ConfigProvider cp = new ConfigProvider();

            String overrideServer = null;
            if (args.containsKey("--ssh") && !args.get("--ssh").equals("true")) {
                overrideServer = args.get("--ssh");
            } else if (args.containsKey("--server")) {
                overrideServer = args.get("--server");
            }

            this.projectConfig = cp.getProjectConfig();

            if (overrideServer != null) {
                
                this.config = cp.getServerConfig(overrideServer);
            } else if (this.projectConfig != null) {
                
                this.config = cp.getServerConfig(this.projectConfig.getServername());
            }

        } catch (Exception e) {
            
        }
    }

    private Session getSession() throws Exception {
        if (config == null) {
            Log.error("No valid server configuration found.");
            Log.error(
                    "Please specify a server using --server <name> or run this command inside a configured project directory.");
            System.exit(1);
        }
        return new DeployService().connectSSH(config);
    }

    public void handle() {
        try {
            
            if (args.containsKey("--version")) {
                System.out.println("WebDeploy v1.2.0");
                return;
            }
            if (args.containsKey("--help")) {
                registry.printHelp();
                return;
            }
            if (args.containsKey("--version")) {
                System.out.println("WebDeploy v1.2.0 - Mega Upgrade");
                return;
            }
            
            Command found = null;
            String foundKey = null;
            for (String k : args.keySet()) {
                found = registry.find(k);
                if (found != null) {
                    foundKey = k;
                    break;
                }
            }

            if (found != null) {
                Session session = null;
                
                if (!foundKey.equals("--self-test") && !foundKey.equals("--setupdomain")
                        && !foundKey.equals("--sftp")) {
                    session = getSession();
                }

                try {
                    found.execute(session, args, projectConfig);
                } finally {
                    if (session != null) {
                        session.disconnect();
                    }
                }
            } else {
                
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
        
        String serverArg = args.get("--ssh");
        Config effectiveConfig = this.config;

        if (serverArg != null && !serverArg.isEmpty() && !serverArg.equals("true")) {
            
            try {
                effectiveConfig = new ConfigProvider().getServerConfig(serverArg);
            } catch (Exception e) {
                Log.error("Server config not found: " + serverArg);
                return;
            }
        }

        if (effectiveConfig == null) {
            Log.error("Project config missing for SSH. Run inside a project or use --ssh <servername>");
            return;
        }
        CommandUtils.runInteractive(effectiveConfig.getKeypath(), effectiveConfig.getUser(), effectiveConfig.getHost(),
                cmd);
    }

}
