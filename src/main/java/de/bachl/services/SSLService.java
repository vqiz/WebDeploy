package de.bachl.services;

import com.jcraft.jsch.Session;
import de.bachl.utils.Log;

public class SSLService {

    public void installCertbot(Session session) throws Exception {
        Log.info("Installing Certbot...");
        // Install snapd if not present (usually present on Ubuntu)
        sendCommand("sudo apt-get update", session);
        sendCommand("sudo apt-get install -y snapd", session);
        sendCommand("sudo snap install core; sudo snap refresh core", session);

        // Install certbot via snap
        sendCommand("sudo snap install --classic certbot", session);
        sendCommand("sudo ln -sf /snap/bin/certbot /usr/bin/certbot", session);
    }

    public void setupSSL(Session session, String domain) throws Exception {
        Log.info("Requesting SSL certificate for " + domain + "...");
        // Run certbot --nginx non-interactively
        // --non-interactive: No prompts
        // --agree-tos: Agree to terms
        // -m: Email for urgent renewal and security notices (asking user for email
        // might be better, but implementing generic for now)
        // --redirect: Force redirect to HTTPS

        // Note: We need an email. For now, we might skip email
        // register-unsafely-without-email or ask user.
        // Let's use register-unsafely-without-email for automation if no email
        // provided, or just fail.
        // Better: Expect user to provide email in args or use a dummy one if acceptable
        // (but not recommended).
        // Let's assume we can ask for specific domain and use
        // --register-unsafely-without-email for simplicity of this "easy setup" tool,
        // OR better: prompt user or use a placeholder if not critical.
        // Let's use --register-unsafely-without-email to make it zero-config for now as
        // per "automaticly can setup".

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
