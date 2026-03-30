/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.setup;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import de.bachl.Config.ConfigProvider;
import de.bachl.Config.ProjectConfig;
import de.bachl.utils.ArgsLister;
import de.bachl.utils.Log;

public class ProjectProvider {
    private final HashMap<String, String> args;

    public ProjectProvider(HashMap<String, String> args) {
        this.args = args;
    }

    public void setup() {
        Log.info("=== WebDeploy Project Setup ===");

        // Detect template
        String template = args.getOrDefault("--template", "");

        // Pre-fill defaults based on template
        String defaultBuildCommand = "";
        String defaultUploadPath = "dist";

        switch (template.toLowerCase()) {
            case "react":
            case "vue":
                defaultBuildCommand = "npm run build";
                defaultUploadPath = "dist";
                break;
            case "node":
                defaultBuildCommand = "";
                defaultUploadPath = ".";
                break;
            case "spring":
                defaultBuildCommand = "./gradlew build";
                defaultUploadPath = "build/libs";
                break;
            case "static":
                defaultBuildCommand = "";
                defaultUploadPath = ".";
                break;
            default:
                break;
        }

        new ArgsLister().print();

        try (Scanner scanner = new Scanner(System.in)) {

        Log.info("Server name:");
        String server = scanner.nextLine().trim();

        if (!new File(System.getProperty("user.home") + "/.webdeploy/servers/" + server).exists()) {
            Log.error("Server '" + server + "' does not exist. Run --setup first.");
            System.exit(1);
        }

        Log.info("Project name:");
        String project = scanner.nextLine().trim();

        if (project.isEmpty()) {
            Log.error("Project name cannot be empty.");
            System.exit(1);
        }

        Log.info("Frontend build command" + (defaultBuildCommand.isEmpty() ? " [leave empty to skip]" : " [default: " + defaultBuildCommand + "]") + ":");
        String buildCommandInput = scanner.nextLine().trim();
        String buildCommand = buildCommandInput.isEmpty() ? defaultBuildCommand : buildCommandInput;

        Log.info("Local folder to upload [default: " + defaultUploadPath + "]:");
        String uploadPathInput = scanner.nextLine().trim();
        String uploadPath = uploadPathInput.isEmpty() ? defaultUploadPath : uploadPathInput;

        Log.info("Domain name (leave empty to skip):");
        String domain = scanner.nextLine().trim();
        boolean enableDomain = !domain.isEmpty();

        Log.info("Nginx reverse proxy? (y/n) [default: n]:");
        String proxyAnswer = scanner.nextLine().trim();

        String backendProxyPath = "";
        String backendProxyTarget = "";
        String backendFilePath = "";

        if (proxyAnswer.equalsIgnoreCase("y")) {
            Log.info("Proxy path (e.g. /api or / for root):");
            backendProxyPath = scanner.nextLine().trim();

            Log.info("Proxy target URL (e.g. http://localhost:8080):");
            backendProxyTarget = scanner.nextLine().trim();

            Log.info("Backend server file to deploy? (y/n):");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                Log.info("Backend file path (e.g. server.js):");
                backendFilePath = scanner.nextLine().trim();
            }
        }

        Log.info("Java/Spring backend deployment? (y/n) [default: n]:");
        String backendAnswer = scanner.nextLine().trim();
        boolean needsbackend = backendAnswer.equalsIgnoreCase("y");

        String backendBuildCommand = "";
        String backendArtifactPath = "";
        String backendDeployPath = "";
        String backendRunCommand = "";
        String backendServiceName = "";
        String backendpath = "";

        if (needsbackend) {
            Log.info("Backend build command (e.g. mvn clean package or ./gradlew build):");
            backendBuildCommand = scanner.nextLine().trim();

            Log.info("Local path to backend artifact (e.g. target/app.jar):");
            backendArtifactPath = scanner.nextLine().trim();

            Log.info("Remote deploy path (e.g. /var/www/backend):");
            backendDeployPath = scanner.nextLine().trim();
            backendpath = backendDeployPath;

            Log.info("Command to run backend (e.g. java -jar app.jar):");
            backendRunCommand = scanner.nextLine().trim();

            Log.info("Systemd service name [default: " + project + "-backend]:");
            backendServiceName = scanner.nextLine().trim();
            if (backendServiceName.isEmpty()) {
                backendServiceName = project + "-backend";
            }
        }

        Log.info("Nginx client_max_body_size [default: 10M]:");
        String clientMaxBodySizeInput = scanner.nextLine().trim();
        String clientMaxBodySize = clientMaxBodySizeInput.isEmpty() ? "10M" : clientMaxBodySizeInput;

        ProjectConfig projectConfig = new ProjectConfig(server, project, needsbackend, backendpath,
                enableDomain, domain, buildCommand, uploadPath,
                backendBuildCommand, backendArtifactPath, backendDeployPath,
                backendRunCommand, backendServiceName, backendProxyPath, backendProxyTarget,
                clientMaxBodySize, backendFilePath);

        new ConfigProvider().setupProject(projectConfig);
        Log.success("Project '" + project + "' configured successfully.");

        if (enableDomain) {
            Log.info("Would you like to run SSL setup now? (y/n):");
            String sslAnswer = scanner.nextLine().trim();
            if (sslAnswer.equalsIgnoreCase("y")) {
                Log.info("Run '--deploy' first to deploy your project, then '--setupdomain' to configure SSL.");
                Log.info("Or run: webdeploy --setupdomain --server=" + server + " --domain=" + domain);
            }
        }

        } // end try-with-resources scanner
    }
}
