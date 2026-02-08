/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.setup;

import com.jcraft.jsch.Session;

import de.bachl.Config.Config;
import de.bachl.Config.ConfigProvider;
import de.bachl.Config.ProjectConfig;
import de.bachl.utils.Log;

public class DeployService {

    private final String dir = System.getProperty("user.dir");

    public void deploy() {
        ConfigProvider configProvider = new ConfigProvider();
        ProjectConfig projectConfig = configProvider.getProjectConfig();
        Config config = configProvider.getServerConfig(projectConfig.getServername());

        if (projectConfig.getBuildCommand() != null && !projectConfig.getBuildCommand().isEmpty()) {
            Log.info("Executing build command: " + projectConfig.getBuildCommand());
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", projectConfig.getBuildCommand());
                pb.inheritIO();
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    Log.error("Build command failed with exit code: " + exitCode);
                    System.exit(1);
                }
                Log.info("Build successful.");
            } catch (Exception e) {
                Log.error("Failed to execute build command: " + e.getMessage());
                System.exit(1);
            }
        }

        String uploadDir = projectConfig.getUploadPath();
        if (uploadDir == null || uploadDir.isEmpty()) {
            uploadDir = "dist";
        }

        Log.info("Start sftp connection for File upload");
        connect(config, dir + "/" + uploadDir, "/var/www/html/" + projectConfig.getProjectname());

        Log.info("Start SSH connection for command execution");
        try {
            Session session = connectSSH(config);

            // Skip Nginx setup on deploy to prevent overwriting SSL config.
            // Nginx is set up via --setupproject (initial) or --setupdomain.
            // if (projectConfig.getDomain() == null || projectConfig.getDomain().isEmpty())
            // {
            // projectConfig.setDomain(domain);
            // }
            // new de.bachl.services.NginxService().setupSite(session, projectConfig);

            Log.info("Start command execution");
            sendCommand("", session);

            if (projectConfig.isNeedsbackend()) {
                deployBackend(session, projectConfig, config);
            }

            // Deploy Node.js backend if configured
            if (projectConfig.getBackendFilePath() != null && !projectConfig.getBackendFilePath().isEmpty()) {
                deployNodeBackend(session, projectConfig, config);
            }

        } catch (Exception e) {
            Log.error("Failed to deploy to " + config.getHost() + ". Error: " + e.getMessage());
            System.exit(1);
        }

    }

    private void deployBackend(Session session, ProjectConfig projectConfig, Config serverConfig) {
        Log.info("--- Starting Backend Deployment ---");

        String buildCmd = projectConfig.getBackendBuildCommand();
        if (buildCmd != null && !buildCmd.isEmpty()) {
            Log.info("Executing backend build: " + buildCmd);
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", buildCmd);
                pb.inheritIO();
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    Log.error("Backend build failed. Exit code: " + exitCode);
                    System.exit(1);
                }
            } catch (Exception e) {
                Log.error("Backend build failed: " + e.getMessage());
                System.exit(1);
            }
        }

        String artifactPath = projectConfig.getBackendArtifactPath();
        String remotePath = projectConfig.getBackendDeployPath();

        if (artifactPath != null && !artifactPath.isEmpty() && remotePath != null && !remotePath.isEmpty()) {
            Log.info("Uploading backend artifact from " + artifactPath + " to " + remotePath);
            try {
                com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();

                try {
                    channelSftp.mkdir(remotePath);
                } catch (Exception e) {
                    /* Ignore if exists */ }

                java.io.File localArtifact = new java.io.File(artifactPath);
                if (localArtifact.isDirectory()) {
                    recursiveUpload(channelSftp, localArtifact, remotePath);
                } else {
                    channelSftp.put(new java.io.FileInputStream(localArtifact),
                            remotePath + "/" + localArtifact.getName());
                }

                channelSftp.disconnect();
                Log.info("Backend upload complete.");
            } catch (Exception e) {
                Log.error("Backend upload failed: " + e.getMessage());
                System.exit(1);
            }
        }

        String serviceName = projectConfig.getBackendServiceName();
        String runCmd = projectConfig.getBackendRunCommand();

        if (serviceName != null && !serviceName.isEmpty() && runCmd != null && !runCmd.isEmpty()) {
            Log.info("Configuring Systemd Service: " + serviceName);

            String serviceContent = "[Unit]\n" +
                    "Description=" + serviceName + " (WebDeploy)\n" +
                    "After=network.target\n" +
                    "\n" +
                    "[Service]\n" +
                    "User=root\n" +
                    "WorkingDirectory=" + remotePath + "\n" +
                    "ExecStart=" + runCmd + "\n" +
                    "Restart=always\n" +
                    "\n" +
                    "[Install]\n" +
                    "WantedBy=multi-user.target\n";

            String remoteServiceFile = "/etc/systemd/system/" + serviceName + ".service";

            try {

                String writeCmd = "echo '" + serviceContent + "' | sudo tee " + remoteServiceFile;
                sendCommand(writeCmd, session);

                sendCommand("sudo systemctl daemon-reload", session);
                sendCommand("sudo systemctl enable " + serviceName, session);
                sendCommand("sudo systemctl restart " + serviceName, session);

                Log.info("Backend service restarted.");
            } catch (Exception e) {
                Log.error("Failed to configure backend service: " + e.getMessage());
            }
        }
    }

    private void deployNodeBackend(Session session, ProjectConfig projectConfig, Config serverConfig) {
        Log.info("--- Starting Node.js Backend Deployment ---");

        String backendFilePath = projectConfig.getBackendFilePath();
        java.io.File backendFile = new java.io.File(backendFilePath);

        if (!backendFile.exists()) {
            Log.error("Backend file not found: " + backendFilePath);
            return;
        }

        String projectRoot = "/var/www/html/" + projectConfig.getProjectname();
        String backendFileName = backendFile.getName();

        try {
            // 1. Check and install Node.js if needed
            Log.info("Checking for Node.js installation...");
            try {
                sendCommand("which node", session);
                Log.info("Node.js is already installed.");
            } catch (Exception e) {
                Log.info("Node.js not found. Installing Node.js 20.x...");
                sendCommand("curl -fsSL https://deb.nodesource.com/setup_20.x | bash -", session);
                sendCommand("apt-get install -y nodejs", session);
                Log.info("Node.js installation complete.");
            }

            // 2. Upload backend file
            Log.info("Uploading backend file: " + backendFileName);
            com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            channelSftp.put(new java.io.FileInputStream(backendFile), projectRoot + "/" + backendFileName);

            // 3. Check and upload package.json if exists
            java.io.File packageJson = new java.io.File(backendFile.getParent(), "package.json");
            if (packageJson.exists()) {
                Log.info("Uploading package.json...");
                channelSftp.put(new java.io.FileInputStream(packageJson), projectRoot + "/package.json");

                channelSftp.disconnect();

                // 4. Install npm dependencies
                Log.info("Installing npm dependencies...");
                sendCommand("cd " + projectRoot + " && npm install --production", session);
            } else {
                channelSftp.disconnect();
            }

            // 5. Kill existing process and start new one
            Log.info("Starting backend service...");
            String killCmd = "pkill -f 'node.*" + backendFileName + "' || true";
            sendCommand(killCmd, session);

            String startCmd = "cd " + projectRoot + " && nohup node " + backendFileName +
                    " > /var/log/" + projectConfig.getProjectname() + "-api.log 2>&1 &";
            sendCommand(startCmd, session);

            // 6. Verify it started
            Thread.sleep(2000);
            Log.info("Backend service started successfully.");

        } catch (Exception e) {
            Log.error("Failed to deploy Node.js backend: " + e.getMessage());
        }
    }

    public com.jcraft.jsch.Session connectSSH(Config config) throws com.jcraft.jsch.JSchException {
        com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
        jsch.addIdentity(config.getKeypath());
        com.jcraft.jsch.Session session = jsch.getSession(config.getUser(), config.getHost(), 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }

    void sendCommand(String command, Session session) throws Exception {
        com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.connect();
        java.io.InputStream in = channel.getInputStream();
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                if (in.read(tmp, 0, 1024) < 0)
                    break;
            }
            if (channel.isClosed()) {
                if (in.available() > 0)
                    continue;
                int exitStatus = channel.getExitStatus();
                if (exitStatus != 0) {
                    throw new Exception(
                            "Command execution failed with exit code: " + exitStatus + " for command: " + command);
                }
                break;
            }
            Thread.sleep(1000);
        }
        channel.disconnect();
    }

    void connect(Config config, String localPath, String remotePath) {
        Log.info("Connecting to " + config.getHost() + " to upload all project files from " + localPath + " to "
                + remotePath);
        try {
            com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();

            if (config.getKeypath() != null && !config.getKeypath().isEmpty()) {
                jsch.addIdentity(config.getKeypath());
            }

            com.jcraft.jsch.Session session = jsch.getSession(config.getUser(), config.getHost(), 22);
            session.setConfig("StrictHostKeyChecking", "no");

            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                session.setPassword(config.getPassword());
            }

            session.connect();
            Log.info("Successfully connected to " + config.getHost());

            String projectName = new java.io.File(remotePath).getName();
            String storageBase = "/var/www/webdeploy/" + projectName;
            String releaseName = String.valueOf(System.currentTimeMillis());
            String releasePath = storageBase + "/releases/" + releaseName;
            String currentLink = storageBase + "/current";
            String publicLink = remotePath;

            sendCommand("mkdir -p " + releasePath, session);

            com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            Log.info("Starting upload to release: " + releaseName);
            recursiveUpload(channelSftp, new java.io.File(localPath), releasePath);
            Log.info("Upload completed.");

            channelSftp.disconnect();

            Log.info("Updating internal current link...");

            sendCommand("ln -sfn " + releasePath + " " + currentLink, session);

            Log.info("Updating public access link...");

            String checkCmd = "if [ -d \"" + publicLink + "\" ] && [ ! -L \"" + publicLink + "\" ]; then mv \""
                    + publicLink + "\" \"" + publicLink + "_backup_" + releaseName + "\"; fi";
            sendCommand(checkCmd, session);

            sendCommand("ln -sfn " + currentLink + " " + publicLink, session);

            Log.info("Start SSH connection for command execution");

            Log.info("Cleaning up old releases...");
            String listCmd = "ls -1t " + storageBase + "/releases/";

            String cleanupCmd = "cd " + storageBase + "/releases && ls -1t | tail -n +3 | xargs -I {} rm -rf {}";

            sendCommand(cleanupCmd, session);
            Log.info("Cleanup completed.");

        } catch (Exception e) {
            Log.error("Failed to deploy to " + config.getHost() + ". Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void recursiveUpload(com.jcraft.jsch.ChannelSftp sftp, java.io.File localFile, String remotePath)
            throws com.jcraft.jsch.SftpException {
        if (localFile.isDirectory()) {

            try {
                sftp.cd(remotePath);
            } catch (com.jcraft.jsch.SftpException e) {

                try {
                    sftp.mkdir(remotePath);
                    sftp.cd(remotePath);
                } catch (com.jcraft.jsch.SftpException ex) {
                    Log.error("Could not create remote directory: " + remotePath);
                    throw ex;
                }
            }

            java.io.File[] files = localFile.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.getName().equals(".git") || file.getName().equals("build")) {
                        continue;
                    }
                    Log.info("Uploading " + file.getName());
                    recursiveUpload(sftp, file, remotePath + "/" + file.getName());
                }
            }

        } else {
            try {
                sftp.put(new java.io.FileInputStream(localFile), remotePath);
                System.out.print(".");
            } catch (java.io.FileNotFoundException e) {
                Log.error("File not found locally: " + localFile.getAbsolutePath());
            }
        }
    }

}
