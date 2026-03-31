/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.services;

import com.jcraft.jsch.Session;
import de.bachl.Config.ProjectConfig;
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

    public void setupSite(Session session, ProjectConfig config) throws Exception {
        String projectName = config.getProjectname();
        Log.info("Setting up Nginx for project: " + projectName);

        String configContent = buildNginxConfig(config);
        String remoteConfigPath = "/etc/nginx/sites-available/" + projectName;

        // Use base64 to safely write the config (avoids single-quote injection issues)
        String b64 = java.util.Base64.getEncoder()
                .encodeToString(configContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        sendCommand("echo '" + b64 + "' | base64 -d | sudo tee " + remoteConfigPath + " > /dev/null", session);

        sendCommand("sudo rm -f /etc/nginx/sites-enabled/" + projectName, session);
        sendCommand("sudo ln -s " + remoteConfigPath + " /etc/nginx/sites-enabled/", session);
        sendCommand("sudo rm -f /etc/nginx/sites-enabled/default", session);

        reload(session);
    }

    /** Package-private — returns the raw nginx config string without SSH, enabling unit testing. */
    String buildNginxConfig(ProjectConfig config) {
        String projectName = config.getProjectname();
        String domain = config.getDomain();

        String content = "server {\n"
                + "    listen 80;\n"
                + "    server_name " + (domain != null && !domain.isEmpty() ? domain : "_") + ";\n"
                + "\n"
                + "    root /var/www/html/" + projectName + ";\n"
                + "    index index.html index.htm;\n"
                + "\n"
                + "    client_max_body_size "
                + (config.getClientMaxBodySize() != null ? config.getClientMaxBodySize() : "10M") + ";\n"
                + "\n"
                + "    if (-f $document_root/maintenance.html) {\n"
                + "        return 503;\n"
                + "    }\n"
                + "    error_page 503 @maintenance;\n"
                + "    location @maintenance {\n"
                + "        rewrite ^(.*)$ /maintenance.html break;\n"
                + "    }\n";

        boolean rootProxied = "/".equals(config.getBackendProxyPath());

        if (!rootProxied) {
            content += "\n"
                    + "    location / {\n"
                    + "        try_files $uri $uri/ /index.html;\n"
                    + "    }\n";
        }

        if (config.getBackendProxyPath() != null && !config.getBackendProxyPath().isEmpty()
                && config.getBackendProxyTarget() != null && !config.getBackendProxyTarget().isEmpty()) {
            content += "\n"
                    + "    location " + config.getBackendProxyPath() + " {\n"
                    + "        proxy_pass " + config.getBackendProxyTarget() + ";\n"
                    + "        proxy_set_header Host $host;\n"
                    + "        proxy_set_header X-Real-IP $remote_addr;\n"
                    + "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"
                    + "        proxy_set_header X-Forwarded-Proto $scheme;\n"
                    + "    }\n";
        }

        if (config.getNginxCustomBlock() != null && !config.getNginxCustomBlock().isEmpty()) {
            content += "\n    " + config.getNginxCustomBlock() + "\n";
        }

        content += "}";
        return content;
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
                    throw new Exception("Command failed (exit " + exitStatus + "): " + command);
                }
                break;
            }
            Thread.sleep(1000);
        }
        channel.disconnect();
    }
}
