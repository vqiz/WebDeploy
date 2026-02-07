package de.bachl.commands.handlers;

import java.util.HashMap;

import de.bachl.Config.ConfigProvider;
import de.bachl.Config.ProjectConfig;
import de.bachl.utils.Log;

public class EditProjectHandler {

    public void handle(HashMap<String, String> args) {
        ConfigProvider configProvider = new ConfigProvider();
        ProjectConfig config = configProvider.getProjectConfig();

        if (config == null) {
            Log.error("No project configuration found in this directory.");
            return;
        }

        boolean changed = false;

        if (args.containsKey("--server")) {
            Log.info("Updating server to: " + args.get("--server"));
            config.setServername(args.get("--server"));
            changed = true;
        }

        if (args.containsKey("--project")) {
            Log.info("Updating project name to: " + args.get("--project"));
            config.setProjectname(args.get("--project"));
            changed = true;
        }

        if (args.containsKey("--domain")) {
            Log.info("Updating domain to: " + args.get("--domain"));
            config.setDomain(args.get("--domain"));
            config.setEnabledomain(true);
            changed = true;
        }

        if (args.containsKey("--build-command")) {
            Log.info("Updating build command to: " + args.get("--build-command"));
            config.setBuildCommand(args.get("--build-command"));
            changed = true;
        }

        if (args.containsKey("--upload-path")) {
            Log.info("Updating upload path to: " + args.get("--upload-path"));
            config.setUploadPath(args.get("--upload-path"));
            changed = true;
        }

        if (args.containsKey("--needs-backend")) {
            boolean val = Boolean.parseBoolean(args.get("--needs-backend"));
            Log.info("Updating needs backend to: " + val);
            config.setNeedsbackend(val);
            changed = true;
        }

        if (args.containsKey("--backend-path")) {
            Log.info("Updating backend path to: " + args.get("--backend-path"));
            config.setBackendpath(args.get("--backend-path"));
            changed = true;
        }

        if (args.containsKey("--enable-domain")) {
            boolean val = Boolean.parseBoolean(args.get("--enable-domain"));
            Log.info("Updating enable domain to: " + val);
            config.setEnabledomain(val);
            changed = true;
        }

        if (changed) {
            configProvider.setupProject(config);
            Log.info("Project configuration updated successfully.");
        } else {
            Log.warn("No changes specified. Use --editproject with arguments like --server, --domain, etc.");
        }
    }
}
