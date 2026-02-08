/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.commands;

import com.jcraft.jsch.Session;

public class CommandUtils {

    public static void sendCommand(String command, Session session, boolean streamOutput) throws Exception {
        com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.connect();

        java.io.InputStream in = channel.getInputStream();
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0)
                    break;
                if (streamOutput)
                    System.out.print(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (in.available() > 0)
                    continue;
                if (channel.getExitStatus() != 0 && !streamOutput) {
                    throw new Exception("Exit code: " + channel.getExitStatus());
                }
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception ee) {
            }
        }
        channel.disconnect();
    }

    public static String runCommandWithOutput(String command, Session session) throws Exception {
        StringBuilder output = new StringBuilder();
        com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.connect();
        java.io.InputStream in = channel.getInputStream();
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0)
                    break;
                output.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (in.available() > 0)
                    continue;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception ee) {
            }
        }
        channel.disconnect();
        return output.toString().trim();
    }

    public static void runInteractive(String keyPath, String user, String host, String remoteCmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("ssh", "-t", "-i", keyPath, user + "@" + host, remoteCmd);
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
    }
}
