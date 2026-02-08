/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.services;

import com.jcraft.jsch.Session;
import de.bachl.utils.Log;

public class SSLService {

    public void installCertbot(Session session) throws Exception {
        Log.info("Installing Certbot...");
        
        sendCommand("sudo apt-get update", session);
        sendCommand("sudo apt-get install -y snapd", session);
        sendCommand("sudo snap install core; sudo snap refresh core", session);

        sendCommand("sudo snap install --classic certbot", session);
        sendCommand("sudo ln -sf /snap/bin/certbot /usr/bin/certbot", session);
    }

    public void setupSSL(Session session, String domain) throws Exception {
        Log.info("Requesting SSL certificate for " + domain + "...");

        String command = "sudo certbot --nginx -d " + domain
                + " --non-interactive --agree-tos --register-unsafely-without-email --redirect";

        sendCommand(command, session);
        Log.info("SSL setup complete for " + domain);
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
