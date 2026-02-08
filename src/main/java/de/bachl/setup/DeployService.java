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

        // Execute Build Command
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

            if (projectConfig.isEnabledomain() || (projectConfig.getBackendProxyPath() != null
                    && !projectConfig.getBackendProxyPath().isEmpty())) {
                String domain = projectConfig.getDomain();
                if (domain == null || domain.isEmpty()) {
                    domain = "_"; // Default server block
                }
                Log.info("Configuring Nginx for project: " + projectConfig.getProjectname());
                // Ensure domain is set in config for NginxService to use
                if (projectConfig.getDomain() == null || projectConfig.getDomain().isEmpty()) {
                    projectConfig.setDomain(domain);
                }
                new de.bachl.services.NginxService().setupSite(session, projectConfig);
            }

            Log.info("Start command execution");
            sendCommand("", session);

            // Backend Deployment
            if (projectConfig.isNeedsbackend()) {
                deployBackend(session, projectConfig, config);
            }

        } catch (Exception e) {
            Log.error("Failed to deploy to " + config.getHost() + ". Error: " + e.getMessage());
            System.exit(1);
        }

    }

    private void deployBackend(Session session, ProjectConfig projectConfig, Config serverConfig) {
        Log.info("--- Starting Backend Deployment ---");

        // 1. Build Backend Locally
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

        // 2. Upload Artifact
        String artifactPath = projectConfig.getBackendArtifactPath();
        String remotePath = projectConfig.getBackendDeployPath();

        if (artifactPath != null && !artifactPath.isEmpty() && remotePath != null && !remotePath.isEmpty()) {
            Log.info("Uploading backend artifact from " + artifactPath + " to " + remotePath);
            try {
                com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();

                // Ensure remote directory exists
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

        // 3. Systemd Service
        String serviceName = projectConfig.getBackendServiceName();
        String runCmd = projectConfig.getBackendRunCommand();

        if (serviceName != null && !serviceName.isEmpty() && runCmd != null && !runCmd.isEmpty()) {
            Log.info("Configuring Systemd Service: " + serviceName);

            // Generate Service File Content
            // Assuming artifact is uploaded to remotePath. working dir = remotePath
            String serviceContent = "[Unit]\n" +
                    "Description=" + serviceName + " (WebDeploy)\n" +
                    "After=network.target\n" +
                    "\n" +
                    "[Service]\n" +
                    "User=root\n" + // Configurable user? Defaulting to root as per current context
                    "WorkingDirectory=" + remotePath + "\n" +
                    "ExecStart=" + runCmd + "\n" +
                    "Restart=always\n" +
                    "\n" +
                    "[Install]\n" +
                    "WantedBy=multi-user.target\n";

            String remoteServiceFile = "/etc/systemd/system/" + serviceName + ".service";

            try {
                // Upload service file
                // Using echo/tee to write file
                String writeCmd = "echo '" + serviceContent + "' | sudo tee " + remoteServiceFile;
                sendCommand(writeCmd, session);

                // Enable and Restart
                sendCommand("sudo systemctl daemon-reload", session);
                sendCommand("sudo systemctl enable " + serviceName, session);
                sendCommand("sudo systemctl restart " + serviceName, session);

                Log.info("Backend service restarted.");
            } catch (Exception e) {
                Log.error("Failed to configure backend service: " + e.getMessage());
            }
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
            String publicLink = remotePath; // /var/www/html/projectname

            // Ensure storage directories exist
            sendCommand("mkdir -p " + releasePath, session);

            com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            Log.info("Starting upload to release: " + releaseName);
            recursiveUpload(channelSftp, new java.io.File(localPath), releasePath);
            Log.info("Upload completed.");

            channelSftp.disconnect();

            // Update internal 'current' symlink
            Log.info("Updating internal current link...");
            // Ensure storageBase exists? mkdir -p releasePath creates it.
            sendCommand("ln -sfn " + releasePath + " " + currentLink, session);

            // Update public symlink /var/www/html/projectname ->
            // /var/www/webdeploy/projectname/current
            Log.info("Updating public access link...");
            // Check if publicLink exists and is a directory (not a symlink)
            // If so, move it away (legacy migration)
            String checkCmd = "if [ -d \"" + publicLink + "\" ] && [ ! -L \"" + publicLink + "\" ]; then mv \""
                    + publicLink + "\" \"" + publicLink + "_backup_" + releaseName + "\"; fi";
            sendCommand(checkCmd, session);

            // Create/Update the public symlink
            sendCommand("ln -sfn " + currentLink + " " + publicLink, session);

            Log.info("Start SSH connection for command execution");

            // Cleanup old releases (Keep current + 1 previous = 2 latest)
            Log.info("Cleaning up old releases...");
            String listCmd = "ls -1t " + storageBase + "/releases/";
            // Using separate channel/method or incorporating output reading.
            // Since we are in 'connect' which uses a generic session, we can use
            // runCommandWithOutput logic if available or implement ad-hoc.
            // Simpler approach: usage of complex Shell command to keep tail.
            // ls -t | tail -n +3 | xargs -I {} rm -rf releases/{}
            // Be careful with paths.
            // Command: cd storageBase/releases && ls -t | tail -n +3 | xargs -I {} rm -rf
            // {}
            String cleanupCmd = "cd " + storageBase + "/releases && ls -1t | tail -n +3 | xargs -I {} rm -rf {}";
            // Check if there are enough releases first to avoid error? xargs handles empty
            // input usually gracefully or we can ignore error.
            // "tail -n +3" outputs lines starting from line 3. If only 1 or 2 lines, output
            // is empty. xargs with empty input does nothing.
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
                    if (file.getName().equals(".git") || file.getName().equals("node_modules")
                            || file.getName().equals("build")) {
                        continue;
                    }
                    Log.info("Uploading " + file.getName());
                    recursiveUpload(sftp, file, remotePath + "/" + file.getName());
                }
            }

        } else {
            try {
                sftp.put(new java.io.FileInputStream(localFile), remotePath);
                System.out.print("."); // Progress indicator
            } catch (java.io.FileNotFoundException e) {
                Log.error("File not found locally: " + localFile.getAbsolutePath());
            }
        }
    }

}
