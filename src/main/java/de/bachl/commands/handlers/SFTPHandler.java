package de.bachl.commands.handlers;

import java.io.File;
import java.util.HashMap;

import com.jcraft.jsch.Session;

import de.bachl.Config.Config;
import de.bachl.Config.ConfigProvider;
import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.utils.Log;

public class SFTPHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() {
                return "--sftp";
            }

            public String getDescription() {
                return "Open SFTP session in new Terminal (Mac only).";
            }

            public void execute(Session session, HashMap<String, String> args, ProjectConfig projectConfig)
                    throws Exception {
                // 1. Check OS
                String os = System.getProperty("os.name").toLowerCase();
                if (!os.contains("mac")) {
                    Log.error("This feature is only available on macOS.");
                    return;
                }

                // 2. Resolve Server Config
                // Check CLI override
                String serverName = null;
                if (args.containsKey("--server")) {
                    serverName = args.get("--server");
                } else if (args.containsKey("--ssh") && !args.get("--ssh").equals("true")) {
                    serverName = args.get("--ssh");
                }

                ConfigProvider cp = new ConfigProvider();
                Config serverConfig = null;

                try {
                    if (serverName != null) {
                        serverConfig = cp.getServerConfig(serverName);
                    } else if (projectConfig != null) {
                        serverConfig = cp.getServerConfig(projectConfig.getServername());
                    } else {
                        // Fallback: Try loading project config manually if passed as null
                        ProjectConfig pc = cp.getProjectConfig();
                        if (pc != null) {
                            serverConfig = cp.getServerConfig(pc.getServername());
                        }
                    }
                } catch (Exception e) {
                    // ConfigProvider exits 1 if not found usually, or throws.
                    // We catch here just in case.
                }

                if (serverConfig == null) {
                    Log.error("No server configuration found.");
                    Log.error("Use --server <name> or run inside a configured project.");
                    return;
                }

                // 3. Construct .duck file content
                String keyPath = serverConfig.getKeypath();

                if (keyPath.startsWith("~")) {
                    keyPath = System.getProperty("user.home") + keyPath.substring(1);
                }

                File keyFile = new File(keyPath);
                if (!keyFile.exists()) {
                    Log.error("SSH Key not found at: " + keyPath);
                    return;
                }

                String absoluteKeyPath = keyFile.getAbsolutePath();

                String duckXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                        +
                        "<plist version=\"1.0\">\n" +
                        "<dict>\n" +
                        "    <key>Protocol</key>\n" +
                        "    <string>sftp</string>\n" +
                        "    <key>Hostname</key>\n" +
                        "    <string>" + serverConfig.getHost() + "</string>\n" +
                        "    <key>Port</key>\n" +
                        "    <string>22</string>\n" +
                        "    <key>Username</key>\n" +
                        "    <string>" + serverConfig.getUser() + "</string>\n" +
                        "    <key>Identity File</key>\n" +
                        "    <string>" + absoluteKeyPath + "</string>\n" +
                        "    <key>Use Public Key Authentication</key>\n" +
                        "    <true/>\n" +
                        "    <key>Nickname</key>\n" +
                        "    <string>" + serverConfig.getName() + " (WebDeploy)</string>\n" +
                        "</dict>\n" +
                        "</plist>";

                // Write to temp file
                String tempDir = System.getProperty("java.io.tmpdir");
                String duckFilePath = tempDir + (tempDir.endsWith("/") ? "" : "/") + "webdeploy-"
                        + serverConfig.getName() + ".duck";

                try {
                    java.io.FileWriter fw = new java.io.FileWriter(duckFilePath);
                    fw.write(duckXml);
                    fw.close();
                } catch (Exception e) {
                    Log.error("Failed to create connection file: " + e.getMessage());
                    return;
                }

                Log.info("Opening Cyberduck for " + serverConfig.getName() + "...");

                String[] cmd = { "open", duckFilePath };

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.inheritIO();
                Process p = pb.start();
                p.waitFor();

                Log.info("Cyberduck launched.");
            }
        });
    }
}
