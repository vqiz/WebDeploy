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
        Log.info("Setting up project");
        new ArgsLister().print();

        try (Scanner scanner = new Scanner(System.in)) {

            Log.info("Please enter a server name:");
            String server = scanner.nextLine();

            if (!new File(System.getProperty("user.home") + "/.webdeploy/servers/" + server).exists()) {
                Log.error("Server " + server + " does not exist");
                System.exit(1);
            }

            Log.info("Please enter a project name:");
            String project = scanner.nextLine();

            Log.info("Do you need a backend? (y/n)");
            String needbackend = scanner.nextLine();

            String backendpath = ""; // Legacy/compatible path

            // Backend Configuration Variables
            String backendBuildCommand = "";
            String backendArtifactPath = "";
            String backendDeployPath = "";
            String backendRunCommand = "";
            String backendServiceName = "";
            String backendProxyPath = "";
            String backendProxyTarget = "";

            if (needbackend.equals("y")) {
                Log.info("--- Backend Configuration ---");

                Log.info("Enter backend local build command (e.g. 'mvn clean package') [Empty to skip]:");
                backendBuildCommand = scanner.nextLine();

                Log.info("Enter local path to backend artifact (e.g. 'target/app.jar'):");
                backendArtifactPath = scanner.nextLine();

                Log.info("Enter remote path to deploy backend (e.g. '/var/www/backend'):");
                backendDeployPath = scanner.nextLine();
                // Use deploy path as the main backend path for consistency
                backendpath = backendDeployPath;

                Log.info("Enter command to run backend (e.g. 'java -jar app.jar'):");
                backendRunCommand = scanner.nextLine();

                Log.info("Enter systemd service name (default: " + project + "-backend):");
                backendServiceName = scanner.nextLine();
                if (backendServiceName.trim().isEmpty()) {
                    backendServiceName = project + "-backend";
                }

                Log.info("Do you want to proxy requests to this backend? (y/n)");
                if (scanner.nextLine().equalsIgnoreCase("y")) {
                    Log.info("Enter path to proxy (e.g. '/api'):");
                    backendProxyPath = scanner.nextLine();

                    Log.info("Enter target URL (e.g. 'http://localhost:8080'):");
                    backendProxyTarget = scanner.nextLine();
                }
            }

            Log.info("Do you want to configure a domain? (y/n)");
            String enableDomain = scanner.nextLine();
            String domain = "";

            if (enableDomain.equalsIgnoreCase("y")) {
                Log.info("Please enter the domain:");
                domain = scanner.nextLine();
            }

            Log.info("Enter frontend build command (leave empty to skip):");
            String buildCommand = scanner.nextLine();

            Log.info("Enter local frontend folder to upload (default: dist):");
            String uploadPath = scanner.nextLine();
            if (uploadPath.trim().isEmpty()) {
                uploadPath = "dist";
            }

            // scanner.close(); // Do not close System.in

            ProjectConfig projectConfig = new ProjectConfig(server, project, needbackend.equals("y"), backendpath,
                    enableDomain.equalsIgnoreCase("y"),
                    domain, buildCommand, uploadPath,
                    backendBuildCommand, backendArtifactPath, backendDeployPath,
                    backendRunCommand, backendServiceName, backendProxyPath, backendProxyTarget);

            new ConfigProvider().setupProject(projectConfig);
        }
        Log.info("Project setup complete");

    }

}
