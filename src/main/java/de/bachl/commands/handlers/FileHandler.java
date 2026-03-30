/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import java.util.HashMap;

import com.jcraft.jsch.Session;

import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.utils.Log;

public class FileHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() { return "--zip-remote"; }
            public String getDescription() { return "Zip a remote folder (usage: --zip-remote <path>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String path = args.get("--zip-remote");
                CommandUtils.sendCommand("zip -r " + path + ".zip " + path, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--unzip-remote"; }
            public String getDescription() { return "Unzip a remote file."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String path = args.get("--unzip-remote");
                CommandUtils.sendCommand("unzip " + path, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--mv"; }
            public String getDescription() { return "Move a remote file (usage: --mv 'src dest')."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String p = args.get("--mv");
                CommandUtils.sendCommand("mv " + p, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cp"; }
            public String getDescription() { return "Copy a remote file."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String p = args.get("--cp");
                CommandUtils.sendCommand("cp -r " + p, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--chown"; }
            public String getDescription() { return "Change owner recursively."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String p = args.get("--chown");
                CommandUtils.sendCommand("sudo chown -R " + p, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--chmod"; }
            public String getDescription() { return "Change permissions recursively."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String p = args.get("--chmod");
                CommandUtils.sendCommand("sudo chmod -R " + p, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--ls"; }
            public String getDescription() { return "List remote directory (usage: --ls <path>, default /var/www)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String path = args.get("--ls");
                if (path == null || path.equals("true") || path.isEmpty()) {
                    path = "/var/www";
                }
                CommandUtils.sendCommand("ls -la " + path, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cat-remote"; }
            public String getDescription() { return "Print a remote file's contents (usage: --cat-remote <path>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String path = args.get("--cat-remote");
                if (path == null || path.equals("true") || path.isEmpty()) {
                    Log.error("Please specify a remote file path.");
                    return;
                }
                CommandUtils.sendCommand("cat " + path, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--tail-remote"; }
            public String getDescription() { return "Tail last 100 lines of a remote file (usage: --tail-remote <path>)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String path = args.get("--tail-remote");
                if (path == null || path.equals("true") || path.isEmpty()) {
                    Log.error("Please specify a remote file path.");
                    return;
                }
                CommandUtils.sendCommand("tail -n 100 " + path, session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--upload"; }
            public String getDescription() { return "Upload local file/dir to remote (usage: --upload local:remote)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String value = args.get("--upload");
                if (value == null || value.equals("true") || !value.contains(":")) {
                    Log.error("Usage: --upload /local/path:/remote/path");
                    return;
                }
                int colonIdx = value.indexOf(':');
                String localPath = value.substring(0, colonIdx);
                String remotePath = value.substring(colonIdx + 1);

                java.io.File localFile = new java.io.File(localPath);
                if (!localFile.exists()) {
                    Log.error("Local path not found: " + localPath);
                    return;
                }

                com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                try {
                    if (localFile.isDirectory()) {
                        uploadRecursive(channelSftp, localFile, remotePath);
                    } else {
                        channelSftp.put(new java.io.FileInputStream(localFile), remotePath);
                    }
                    Log.success("Upload complete: " + localPath + " -> " + remotePath);
                } finally {
                    channelSftp.disconnect();
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--download"; }
            public String getDescription() { return "Download remote file to local (usage: --download remote:local)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String value = args.get("--download");
                if (value == null || value.equals("true") || !value.contains(":")) {
                    Log.error("Usage: --download /remote/path:/local/path");
                    return;
                }
                int colonIdx = value.indexOf(':');
                String remotePath = value.substring(0, colonIdx);
                String localPath = value.substring(colonIdx + 1);

                com.jcraft.jsch.ChannelSftp channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                try {
                    channelSftp.get(remotePath, localPath);
                    Log.success("Download complete: " + remotePath + " -> " + localPath);
                } finally {
                    channelSftp.disconnect();
                }
            }
        });
    }

    private void uploadRecursive(com.jcraft.jsch.ChannelSftp sftp, java.io.File localFile, String remotePath)
            throws Exception {
        if (localFile.isDirectory()) {
            try {
                sftp.mkdir(remotePath);
            } catch (Exception ignored) { /* already exists */ }
            java.io.File[] files = localFile.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    uploadRecursive(sftp, f, remotePath + "/" + f.getName());
                }
            }
        } else {
            sftp.put(new java.io.FileInputStream(localFile), remotePath);
        }
    }
}
