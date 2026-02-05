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

        Scanner scanner = new Scanner(System.in);

        Log.info("Please enter a server name:");
        String server = scanner.nextLine();

        if (!new File("servers/" + server).exists()) {
            Log.error("Server " + server + " does not exist");
            System.exit(1);
        }

        Log.info("Please enter a project name:");
        String project = scanner.nextLine();

        Log.info("Do you need a backend? (y/n)");
        String needbackend = scanner.nextLine();

        String backendpath = "";
        if (needbackend.equals("y")) {
            Log.info("Please enter a backend path:");
            backendpath = scanner.nextLine();
        }

        Log.info("Do you want to configure a domain? (y/n)");
        String enableDomain = scanner.nextLine();
        String domain = "";

        if (enableDomain.equalsIgnoreCase("y")) {
            Log.info("Please enter the domain:");
            domain = scanner.nextLine();
        }

        // scanner.close(); // Do not close System.in

        ProjectConfig projectConfig = new ProjectConfig(server, project, needbackend.equals("y"), backendpath,
                enableDomain.equalsIgnoreCase("y"),
                domain);

        new ConfigProvider().setupProject(projectConfig);
        Log.info("Project setup complete");

    }

}
