package de.bachl.services;

import com.jcraft.jsch.Session;
import de.bachl.utils.Log;

public class NginxService {

    public void install(Session session) throws Exception {
        Log.info("Installing Nginx...");
        sendCommand("sudo apt-get update", session);
        sendCommand("sudo apt-get install -y nginx", session);
        Log.info("Starting Nginx...");
        sendCommand("sudo systemctl start nginx", session);
        sendCommand("sudo systemctl enable nginx", session);
    }

    public void setupSite(Session session, String projectName, String domain, int port) throws Exception {
        Log.info("Setting up Nginx for project: " + projectName);

        // Define the config content
        String config = "server {\n" +
                "    listen 80;\n" +
                "    server_name " + domain + ";\n" +
                "\n" +
                "    root /var/www/html/" + projectName + ";\n" +
                "    index index.html index.htm;\n" +
                "\n" +
                "    location / {\n" +
                "        try_files $uri $uri/ =404;\n" +
                "    }\n" +
                "}";

        // Write the config file
        String remoteConfigPath = "/etc/nginx/sites-available/" + projectName;
        String echoCommand = "echo '" + config + "' | sudo tee " + remoteConfigPath;
        sendCommand(echoCommand, session);

        // Enable the site (symlink)
        String linkCommand = "sudo ln -s " + remoteConfigPath + " /etc/nginx/sites-enabled/";
        // Check if link exists first to avoid error or force it
        sendCommand("sudo rm -f /etc/nginx/sites-enabled/" + projectName, session);
        sendCommand(linkCommand, session);

        // Remove default if it exists and clutters (optional, but good for single site
        // setup if wanted)
        // sendCommand("sudo rm -f /etc/nginx/sites-enabled/default", session);

        reload(session);
    }

    public void reload(Session session) throws Exception {
        Log.info("Reloading Nginx...");
        sendCommand("sudo systemctl reload nginx", session);
    }

    private void sendCommand(String command, Session session) throws Exception {
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
}
