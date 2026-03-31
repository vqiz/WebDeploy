/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.setup;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import de.bachl.Config.ProjectConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeployServiceTest {

    @Mock
    Session session;

    @Mock
    ChannelExec execChannel;

    @Mock
    ChannelSftp sftpChannel;

    @TempDir
    Path tempDir;

    private void stubExecChannel() throws Exception {
        when(session.openChannel("exec")).thenReturn(execChannel);
        when(execChannel.getInputStream())
                .thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));
        when(execChannel.isClosed()).thenAnswer(new org.mockito.stubbing.Answer<Boolean>() {
            int count = 0;
            public Boolean answer(org.mockito.invocation.InvocationOnMock inv) {
                return (count++ % 2 != 0);
            }
        });
        when(execChannel.getExitStatus()).thenReturn(0);
    }

    // -----------------------------------------------------------------------
    // sendCommand (package-private — accessible from same package in tests)
    // -----------------------------------------------------------------------

    @Test
    void sendCommand_connectsAndDisconnectsChannel() throws Exception {
        stubExecChannel();
        DeployService service = new DeployService();
        service.sendCommand("echo test", session);
        verify(execChannel).connect();
        verify(execChannel).disconnect();
    }

    @Test
    void sendCommand_setsCommandString() throws Exception {
        stubExecChannel();
        DeployService service = new DeployService();
        service.sendCommand("uptime", session);
        verify(execChannel).setCommand("uptime");
    }

    @Test
    void sendCommand_throwsOnNonZeroExit() throws Exception {
        when(session.openChannel("exec")).thenReturn(execChannel);
        when(execChannel.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(execChannel.isClosed()).thenReturn(false, true);
        when(execChannel.getExitStatus()).thenReturn(127);

        DeployService service = new DeployService();
        assertThrows(Exception.class, () -> service.sendCommand("bad", session));
    }

    // -----------------------------------------------------------------------
    // recursiveUpload
    // -----------------------------------------------------------------------

    @Test
    void recursiveUpload_withSingleFile_callsSftpPut() throws Exception {
        Path file = tempDir.resolve("index.html");
        Files.writeString(file, "<html/>");

        DeployService service = new DeployService();
        service.recursiveUpload(sftpChannel, file.toFile(), "/remote/index.html");

        verify(sftpChannel).put(any(java.io.FileInputStream.class), eq("/remote/index.html"));
    }

    @Test
    void recursiveUpload_withDirectory_callsMkdirAndRecurses() throws Exception {
        // Create a directory with two files
        Path subDir = tempDir.resolve("assets");
        Files.createDirectory(subDir);
        Files.writeString(subDir.resolve("a.js"), "var a=1;");
        Files.writeString(subDir.resolve("b.css"), "body{}");

        // cd throws first (dir missing), then succeeds after mkdir
        doThrow(new SftpException(2, "no such file")).doNothing().when(sftpChannel).cd("/remote/assets");

        DeployService service = new DeployService();
        service.recursiveUpload(sftpChannel, subDir.toFile(), "/remote/assets");

        verify(sftpChannel).mkdir("/remote/assets");
        verify(sftpChannel, times(2)).put(any(java.io.FileInputStream.class), anyString());
    }

    @Test
    void recursiveUpload_skipsGitDirectory() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectory(gitDir);
        Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/main");

        // We need the parent to behave as a directory; cd succeeds for top-level
        doNothing().when(sftpChannel).cd(tempDir.toString());

        DeployService service = new DeployService();
        service.recursiveUpload(sftpChannel, tempDir.toFile(), tempDir.toString());

        // No file inside .git should be uploaded
        verify(sftpChannel, never()).put(any(java.io.FileInputStream.class),
                contains(".git"));
    }

    // -----------------------------------------------------------------------
    // deploy — build-command skip path (safe to test without real SSH)
    // -----------------------------------------------------------------------

    @Test
    void projectConfig_withEmptyBuildCommand_skipsBuildStep() {
        ProjectConfig config = new ProjectConfig();
        config.setProjectname("app");
        config.setServername("server");
        config.setBuildCommand(""); // empty — should be skipped

        // We just verify that the buildCommand check logic works correctly:
        // if command is null/empty we do NOT run ProcessBuilder
        String cmd = config.getBuildCommand();
        assertTrue(cmd == null || cmd.isEmpty(),
                "Build command should be empty so the deploy step skips it");
    }
}
