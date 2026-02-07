package de.bachl.setup;

import java.util.HashMap;

import com.jcraft.jsch.Session;

import de.bachl.Config.Config;
import de.bachl.Config.ConfigProvider;
import de.bachl.utils.Log;

public class SetupProvider {

    private final HashMap<String, String> args;
    private String host;
    private String password;
    private String user = "root";

    public SetupProvider(HashMap<String, String> args) {
        this.args = args;
    }

    public void setup() {
        fetchdata();
        connect();

    }

    void connect() {
        String hostname = this.host;
        int port = 22;

        if (hostname.contains(":")) {
            String[] parts = hostname.split(":");
            hostname = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        try {
            com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
            com.jcraft.jsch.Session session = jsch.getSession(this.user, hostname, port);
            session.setPassword(this.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // setup Config
            Log.info("Enter server name: ");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String serverName = scanner.nextLine();
            scanner.close();

            // setup nginx
            new de.bachl.services.NginxService().install(session);

            // setup auth
            Log.info("setting up auth");
            String keypath = setupAuth(session, serverName);

            // setup config
            Log.info("setting up config");
            Config config = new Config(keypath, serverName, this.host, this.user, this.password);
            new ConfigProvider().setupServer(config);

            Log.info("Setup completed successfully");
            session.disconnect();
            Log.info("Disconnected from " + hostname);

        } catch (Exception e) {
            Log.error("Failed to establish SSH connection to " + hostname + ": " + e.getMessage());
            System.exit(1);
        }

    }

    void sendCommand(String command, Session session) throws Exception {
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

    void fetchdata() {
        if (!args.containsKey("--host") || !args.containsKey("--password")) {
            Log.error("Missing critical arguments --host or --password");
            System.exit(1);
        }
        if (args.containsKey("--user")) {
            this.user = args.get("--user");
        }
        this.host = args.get("--host");
        this.password = args.get("--password");
    }

    String setupAuth(Session session, String name) throws Exception {
        Log.info("Setting up SSH Key Authentication...");

        com.jcraft.jsch.KeyPair kpair = com.jcraft.jsch.KeyPair.genKeyPair(new com.jcraft.jsch.JSch(),
                com.jcraft.jsch.KeyPair.RSA);

        String keyPath = System.getProperty("user.home") + "/.ssh/id_rsa_" + name;

        java.io.File sshDir = new java.io.File(System.getProperty("user.home") + "/.ssh");
        if (!sshDir.exists()) {
            sshDir.mkdirs();
        }

        kpair.writePrivateKey(keyPath);
        kpair.writePublicKey(keyPath + ".pub", "webdeploy-auto-generated");

        // Fix permissions for private key
        try {
            new ProcessBuilder("chmod", "600", keyPath).start().waitFor();
        } catch (Exception e) {
            Log.warn("Could not set 600 permissions on private key: " + e.getMessage());
        }

        Log.info("Generated SSH keys at: " + keyPath);

        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        kpair.writePublicKey(bos, "");
        String publicKey = bos.toString("UTF-8").trim();

        sendCommand("mkdir -p ~/.ssh", session);
        sendCommand("chmod 700 ~/.ssh", session);
        sendCommand("echo '" + publicKey + "' >> ~/.ssh/authorized_keys", session);
        sendCommand("chmod 600 ~/.ssh/authorized_keys", session);

        // Backup config first (kept for safety context)
        sendCommand("sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_config.bak", session);
        sendCommand("sudo sed -i 's/^.*PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config",
                session);
        sendCommand("sudo sed -i 's/^.*PubkeyAuthentication.*/PubkeyAuthentication yes/' /etc/ssh/sshd_config",
                session);

        try {
            sendCommand("sudo sshd -t", session);
            try {
                sendCommand("sudo systemctl restart ssh", session);
                Log.info("SSH configuration updated and service restarted.");
            } catch (Exception e) {
                // Fallback for distros where service is named sshd (like CentOS/RHEL)
                try {
                    sendCommand("sudo systemctl restart sshd", session);
                    Log.info("SSH configuration updated and service restarted (via sshd).");
                } catch (Exception ex) {
                    Log.warn("SSH restart command execution completed, but might have disconnected session. "
                            + ex.getMessage());
                }
            }
        } catch (Exception e) {
            Log.error(
                    "SSH configuration test failed! Not restarting service to avoid lockout. Error: " + e.getMessage());
        }
        return keyPath;
    }
}
