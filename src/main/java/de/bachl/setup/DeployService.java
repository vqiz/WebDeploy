/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

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

        // Run pre-deploy command locally if configured
        if (projectConfig.getPreDeployCommand() != null && !projectConfig.getPreDeployCommand().isEmpty()) {
            Log.info("Running pre-deploy command: " + projectConfig.getPreDeployCommand());
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", projectConfig.getPreDeployCommand());
                pb.inheritIO();
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    Log.error("Pre-deploy command failed with exit code: " + exitCode);
                    System.exit(1);
                }
                Log.success("Pre-deploy command completed.");
            } catch (Exception e) {
                Log.error("Failed to run pre-deploy command: " + e.getMessage());
                System.exit(1);
            }
        }

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
                Log.success("Build successful.");
            } catch (Exception e) {
                Log.error("Failed to execute build command: " + e.getMessage());
                System.exit(1);
            }
        }

        String uploadDir = projectConfig.getUploadPath();
        if (uploadDir == null || uploadDir.isEmpty()) {
            uploadDir = "dist";
        }

        Log.info("Starting SFTP connection for file upload");
        connect(config, dir + "/" + uploadDir, "/var/www/html/" + projectConfig.getProjectname(), projectConfig);

        Log.info("Starting SSH connection for command execution");
        try {
            Session session = connectSSH(config);

            // Setup Nginx if not yet configured
            try {
                String nginxCheck = "ls /etc/nginx/sites-available/" + projectConfig.getProjectname();
                de.bachl.commands.CommandUtils.sendCommand(nginxCheck, session, false);
                Log.info("Nginx config already exists, skipping setup.");
            } catch (Exception e) {
                if (projectConfig.getDomain() != null && !projectConfig.getDomain().isEmpty()) {
                    Log.info("Nginx config not found, setting up now...");
                    new de.bachl.services.NginxService().setupSite(session, projectConfig);
                }
            }

            if (projectConfig.isNeedsbackend()) {
                deployBackend(session, projectConfig, config);
            }

            if (projectConfig.getBackendFilePath() != null && !projectConfig.getBackendFilePath().isEmpty()) {
                deployNodeBackend(session, projectConfig, config);
            }

            session.disconnect();

        } catch (Exception e) {
            Log.error("Failed to deploy to " + config.getHost() + ". Error: " + e.getMessage());
            System.exit(1);
        }

        // Run post-deploy command locally if configured
        if (projectConfig.getPostDeployCommand() != null && !projectConfig.getPostDeployCommand().isEmpty()) {
            Log.info("Running post-deploy command: " + projectConfig.getPostDeployCommand());
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", projectConfig.getPostDeployCommand());
                pb.inheritIO();
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    Log.error("Post-deploy command failed with exit code: " + exitCode);
                } else {
                    Log.success("Post-deploy command completed.");
                }
            } catch (Exception e) {
                Log.error("Failed to run post-deploy command: " + e.getMessage());
            }
        }

        // Health check
        String healthUrl = projectConfig.getHealthCheckUrl();
        if (healthUrl == null || healthUrl.isEmpty()) {
            if (projectConfig.getDomain() != null && !projectConfig.getDomain().isEmpty()) {
                healthUrl = "http://" + projectConfig.getDomain() + "/";
            }
        }
        if (healthUrl != null && !healthUrl.isEmpty()) {
            final String finalHealthUrl = healthUrl;
            Log.info("Performing health check on: " + finalHealthUrl);
            try {
                Thread.sleep(3000);
                ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                        "curl -sSf " + finalHealthUrl + " --max-time 10 && echo 'Health check passed' || echo 'Health check FAILED'");
                pb.inheritIO();
                pb.start().waitFor();
            } catch (Exception e) {
                Log.warn("Health check error: " + e.getMessage());
            }
        }

        Log.success("Deployment complete.");
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
                    /* Ignore if exists */
                }

                java.io.File localArtifact = new java.io.File(artifactPath);
                if (localArtifact.isDirectory()) {
                    recursiveUpload(channelSftp, localArtifact, remotePath);
                } else {
                    channelSftp.put(new java.io.FileInputStream(localArtifact),
                            remotePath + "/" + localArtifact.getName());
                }

                channelSftp.disconnect();
                Log.success("Backend upload complete.");
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
                // Use base64 to safely write the service file
                String b64 = java.util.Base64.getEncoder()
                        .encodeToString(serviceContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String writeCmd = "echo '" + b64 + "' | base64 -d | sudo tee " + remoteServiceFile + " > /dev/null";
                sendCommand(writeCmd, session);

                sendCommand("sudo systemctl daemon-reload", session);
                sendCommand("sudo systemctl enable " + serviceName, session);
                sendCommand("sudo systemctl restart " + serviceName, session);

                Log.success("Backend service restarted.");
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

            Log.info("Uploading backend file: " + backendFileName);
            com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.put(new java.io.FileInputStream(backendFile), projectRoot + "/" + backendFileName);

            java.io.File packageJson = new java.io.File(backendFile.getParent(), "package.json");
            if (packageJson.exists()) {
                Log.info("Uploading package.json...");
                channelSftp.put(new java.io.FileInputStream(packageJson), projectRoot + "/package.json");
                channelSftp.disconnect();
                Log.info("Installing npm dependencies...");
                sendCommand("cd " + projectRoot + " && npm install --production", session);
            } else {
                channelSftp.disconnect();
            }

            Log.info("Starting backend service...");
            String killCmd = "pkill -f 'node.*" + backendFileName + "' || true";
            sendCommand(killCmd, session);

            String startCmd = "cd " + projectRoot + " && nohup node " + backendFileName +
                    " > /var/log/" + projectConfig.getProjectname() + "-api.log 2>&1 &";
            sendCommand(startCmd, session);

            Thread.sleep(2000);
            Log.success("Backend service started successfully.");

        } catch (Exception e) {
            Log.error("Failed to deploy Node.js backend: " + e.getMessage());
        }
    }

    public com.jcraft.jsch.Session connectSSH(Config config) throws com.jcraft.jsch.JSchException {
        com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
        jsch.addIdentity(config.getKeypath());
        int port = config.getSshPort();
        com.jcraft.jsch.Session session = jsch.getSession(config.getUser(), config.getHost(), port);
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

    void connect(Config config, String localPath, String remotePath, ProjectConfig projectConfig) {
        Log.info("Connecting to " + config.getHost() + " to upload files from " + localPath + " to " + remotePath);
        try {
            com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();

            if (config.getKeypath() != null && !config.getKeypath().isEmpty()) {
                jsch.addIdentity(config.getKeypath());
            }

            int port = config.getSshPort();
            com.jcraft.jsch.Session session = jsch.getSession(config.getUser(), config.getHost(), port);
            session.setConfig("StrictHostKeyChecking", "no");

            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                session.setPassword(config.getPassword());
            }

            session.connect();
            Log.info("Connected to " + config.getHost());

            String projectName = new java.io.File(remotePath).getName();
            String storageBase = "/var/www/webdeploy/" + projectName;
            String releaseName = String.valueOf(System.currentTimeMillis());
            String releasePath = storageBase + "/releases/" + releaseName;
            String currentLink = storageBase + "/current";
            String publicLink = remotePath;

            sendCommand("mkdir -p " + releasePath, session);

            com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            Log.info("Uploading to release: " + releaseName);
            recursiveUpload(channelSftp, new java.io.File(localPath), releasePath);
            Log.success("Upload completed.");

            channelSftp.disconnect();

            Log.info("Updating current symlink...");
            sendCommand("ln -sfn " + releasePath + " " + currentLink, session);

            Log.info("Updating public access link...");
            String checkCmd = "if [ -d \"" + publicLink + "\" ] && [ ! -L \"" + publicLink + "\" ]; then mv \""
                    + publicLink + "\" \"" + publicLink + "_backup_" + releaseName + "\"; fi";
            sendCommand(checkCmd, session);
            sendCommand("ln -sfn " + currentLink + " " + publicLink, session);

            // Cleanup old releases using keepReleases from config
            int keepReleases = (projectConfig != null) ? projectConfig.getKeepReleases() : 5;
            Log.info("Cleaning up old releases (keeping last " + keepReleases + ")...");
            String cleanupCmd = "cd " + storageBase + "/releases && ls -1t | tail -n +" + (keepReleases + 1) + " | xargs -I {} rm -rf {}";
            sendCommand(cleanupCmd, session);
            Log.success("Cleanup completed.");

            session.disconnect();

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
