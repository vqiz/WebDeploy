package de.bachl.services;

import com.jcraft.jsch.Session;
import de.bachl.utils.Log;
import de.bachl.Config.ProjectConfig;

public class NginxService {

    public void install(Session session) throws Exception {
        Log.info("Installing Nginx...");
        sendCommand("sudo apt-get update", session);
        sendCommand("sudo apt-get install -y nginx", session);
        Log.info("Starting Nginx...");
        sendCommand("sudo systemctl start nginx", session);
        sendCommand("sudo systemctl enable nginx", session);
    }

    public void setupSite(Session session, ProjectConfig config) throws Exception {
        String projectName = config.getProjectname();
        String domain = config.getDomain();

        Log.info("Setting up Nginx for project: " + projectName);

        // Define the config content
        String configContent = "server {\n" +
                "    listen 80;\n" +
                "    server_name " + domain + ";\n" +
                "\n" +
                "    root /var/www/html/" + projectName + ";\n" +
                "    index index.html index.htm;\n" +
                "\n" +
                "    if (-f $document_root/maintenance.html) {\n" +
                "        return 503;\n" +
                "    }\n" +
                "    error_page 503 @maintenance;\n" +
                "    location @maintenance {\n" +
                "        rewrite ^(.*)$ /maintenance.html break;\n" +
                "    }\n" +
                "\n" +
                "    location / {\n" +
                "        try_files $uri $uri/ =404;\n" +
                "    }\n";

        // Proxy Configuration
        if (config.getBackendProxyPath() != null && !config.getBackendProxyPath().isEmpty() &&
                config.getBackendProxyTarget() != null && !config.getBackendProxyTarget().isEmpty()) {

            configContent += "\n" +
                    "    location " + config.getBackendProxyPath() + " {\n" +
                    "        proxy_pass " + config.getBackendProxyTarget() + ";\n" +
                    "        proxy_set_header Host $host;\n" +
                    "        proxy_set_header X-Real-IP $remote_addr;\n" +
                    "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n" +
                    "        proxy_set_header X-Forwarded-Proto $scheme;\n" +
                    "    }\n";
        }

        configContent += "}";

        // Write the config file
        String remoteConfigPath = "/etc/nginx/sites-available/" + projectName;
        String echoCommand = "echo '" + configContent + "' | sudo tee " + remoteConfigPath;
        sendCommand(echoCommand, session);

        // Enable the site (symlink)
        String linkCommand = "sudo ln -s " + remoteConfigPath + " /etc/nginx/sites-enabled/";
        // Check if link exists first to avoid error or force it
        sendCommand("sudo rm -f /etc/nginx/sites-enabled/" + projectName, session);
        sendCommand(linkCommand, session);

        // Remove default if it exists and clutters (optional)
        sendCommand("sudo rm -f /etc/nginx/sites-enabled/default", session);

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
