/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.commands.handlers;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import com.jcraft.jsch.Session;

import de.bachl.Config.Config;
import de.bachl.Config.ConfigProvider;
import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.services.NginxService;
import de.bachl.services.SSLService;
import de.bachl.setup.DeployService;
import de.bachl.utils.Log;

public class DomainHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() {
                return "--setupdomain";
            }

            public String getDescription() {
                return "Configure a domain for a project (Nginx + SSL).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {

                if (session == null) {
                    session = connectInteractive(args);
                }

                if (session == null) {
                    Log.error("Could not establish connection.");
                    return;
                }

                boolean weCreatedSession = true; 

                try {
                    Scanner scanner = new Scanner(System.in);

                    Log.info("Please enter the domain (e.g., example.com):");
                    String domain = scanner.nextLine().trim();

                    if (domain.isEmpty()) {
                        Log.error("Domain cannot be empty.");
                        if (weCreatedSession)
                            session.disconnect();
                        return;
                    }

                    Log.info("Please enter the project name on the server:");
                    String projectName = scanner.nextLine().trim();

                    if (projectName.isEmpty()) {
                        Log.error("Project name cannot be empty.");
                        if (weCreatedSession)
                            session.disconnect();
                        return;
                    }

                    String checkPath = "/var/www/webdeploy/" + projectName;
                    Log.info("Verifying project at " + checkPath + "...");
                    try {
                        
                        CommandUtils.sendCommand("ls -d " + checkPath, session, false);
                    } catch (Exception e) {
                        Log.error("Project directory not found on server: " + checkPath);
                        Log.error("Please ensure the project '" + projectName + "' is deployed first.");
                        if (weCreatedSession)
                            session.disconnect();
                        return;
                    }

                    Log.info("Configuring Nginx for domain: " + domain);

                    ProjectConfig domainConfig = (config != null) ? config : new ProjectConfig();

                    domainConfig.setProjectname(projectName);
                    domainConfig.setDomain(domain);
                    new NginxService().setupSite(session, domainConfig);

                    Log.info("Setting up SSL with Certbot...");
                    new SSLService().installCertbot(session);
                    new SSLService().setupSSL(session, domain);

                    Log.info("Domain setup complete! Access your site at https://" + domain);

                } catch (Exception e) {
                    Log.error("Error setting up domain: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (weCreatedSession && session != null && session.isConnected()) {
                        session.disconnect();
                    }
                }
            }
        });
    }

    private Session connectInteractive(HashMap<String, String> args) throws Exception {
        ConfigProvider cp = new ConfigProvider();

        String targetServer = null;
        if (args.containsKey("--server")) {
            targetServer = args.get("--server");
        } else if (args.containsKey("--ssh") && !args.get("--ssh").equals("true")) {
            targetServer = args.get("--ssh");
        }

        if (targetServer != null) {
            try {
                Config config = cp.getServerConfig(targetServer);
                Log.info("Connecting to " + targetServer + "...");
                return new DeployService().connectSSH(config);
            } catch (Exception e) {
                Log.error("Server config not found for: " + targetServer);
                return null;
            }
        }

        File serverDir = new File(System.getProperty("user.home") + "/.webdeploy/servers/");
        if (!serverDir.exists() || serverDir.listFiles() == null || serverDir.listFiles().length == 0) {
            Log.error("No servers configured. Run --setup first.");
            return null;
        }

        String[] servers = serverDir.list();
        System.out.println("Available Servers:");
        for (int i = 0; i < servers.length; i++) {
            if (!servers[i].startsWith(".")) {
                System.out.println("- " + servers[i]);
            }
        }

        System.out.println("Enter server name to connect to:");
        Scanner scanner = new Scanner(System.in);
        String selectedServer = scanner.nextLine().trim();

        Config config = null;
        try {
            config = cp.getServerConfig(selectedServer);
        } catch (Exception e) {
            Log.error("Server not found.");
            return null;
        }

        Log.info("Connecting to " + selectedServer + "...");
        return new DeployService().connectSSH(config);
    }
}
