package de.bachl.Config;

import java.io.File;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.bachl.utils.Log;

public class ConfigProvider {

    private final FileWriter fileWriter = new FileWriter();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String dir = System.getProperty("user.dir");

    public void setupServer(Config config) {
        Log.info("Writing Config File for " + config.getName());
        new File(System.getProperty("user.home") + "/.webdeploy/servers/").mkdirs();
        fileWriter.writeFile(System.getProperty("user.home") + "/.webdeploy/servers/" + config.getName(),
                gson.toJson(config));
    }

    public void setupProject(ProjectConfig config) {
        Log.info("Writing Project Config File for " + config.getProjectname());
        fileWriter.writeFile(dir + "/webdeploy.config", gson.toJson(config));
    }

    public ProjectConfig getProjectConfig() {
        if (!new File(dir + "/webdeploy.config").exists()) {
            Log.error("No config file found");
            System.exit(1);
        }
        return gson.fromJson(fileWriter.readFile(dir + "/webdeploy.config"), ProjectConfig.class);
    }

    public Config getServerConfig(String servername) {
        if (!new File(System.getProperty("user.home") + "/.webdeploy/servers/" + servername).exists()) {
            Log.error("No Server config file found");
            System.exit(1);
        }
        return gson.fromJson(fileWriter.readFile(System.getProperty("user.home") + "/.webdeploy/servers/" + servername),
                Config.class);
    }

}
