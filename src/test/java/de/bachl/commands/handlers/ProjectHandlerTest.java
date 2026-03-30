/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import de.bachl.Config.ProjectConfig;
import de.bachl.commands.CommandRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectHandlerTest {

    @Mock
    Session session;

    @Mock
    ChannelExec execChannel;

    @Mock
    ChannelSftp sftpChannel;

    private CommandRegistry registry;

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outBuf;
    private ByteArrayOutputStream errBuf;

    @BeforeEach
    void setUp() throws Exception {
        registry = new CommandRegistry();
        new ProjectHandler().register(registry);

        when(session.openChannel("exec")).thenReturn(execChannel);
        when(execChannel.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));
        when(execChannel.isClosed()).thenAnswer(new org.mockito.stubbing.Answer<Boolean>() {
            int count = 0;
            public Boolean answer(org.mockito.invocation.InvocationOnMock inv) {
                return (count++ % 2 != 0);
            }
        });
        when(execChannel.getExitStatus()).thenReturn(0);

        outBuf = new ByteArrayOutputStream();
        errBuf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuf));
        System.setErr(new PrintStream(errBuf));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    @Test
    void backup_isRegistered() {
        assertNotNull(registry.find("--backup"));
    }

    @Test
    void rollback_isRegistered() {
        assertNotNull(registry.find("--rollback"));
    }

    @Test
    void envSet_isRegistered() {
        assertNotNull(registry.find("--env-set"));
    }

    @Test
    void envGet_isRegistered() {
        assertNotNull(registry.find("--env-get"));
    }

    @Test
    void deleteProject_isRegistered() {
        assertNotNull(registry.find("--delete-project"));
    }

    @Test
    void cronList_isRegistered() {
        assertNotNull(registry.find("--cron-list"));
    }

    // -----------------------------------------------------------------------
    // --rollback
    // -----------------------------------------------------------------------

    @Test
    void rollback_withNoReleases_warnsUser() throws Exception {
        // runCommandWithOutput returns empty string — no releases
        when(execChannel.getInputStream()).thenReturn(
                new ByteArrayInputStream(new byte[0]));

        ProjectConfig config = new ProjectConfig();
        config.setProjectname("app");

        HashMap<String, String> args = new HashMap<>();
        registry.find("--rollback").execute(session, args, config);

        String combined = outBuf.toString() + errBuf.toString();
        assertTrue(combined.contains("No releases found") || combined.contains("[ERROR]"),
                "Should warn when no releases are found");
    }

    @Test
    void rollback_withOnlyOneRelease_warnsNoPreviousRelease() throws Exception {
        // First runCommandWithOutput (ls releases) returns one release
        // Second (readlink current) returns same release
        byte[] singleRelease = "1700000000000\n".getBytes();
        byte[] currentLink = "/var/www/webdeploy/app/releases/1700000000000\n".getBytes();

        when(execChannel.getInputStream())
                .thenReturn(new ByteArrayInputStream(singleRelease))
                .thenReturn(new ByteArrayInputStream(currentLink));
        when(execChannel.isClosed()).thenReturn(false, true, false, true);

        ProjectConfig config = new ProjectConfig();
        config.setProjectname("app");

        HashMap<String, String> args = new HashMap<>();
        registry.find("--rollback").execute(session, args, config);

        String combined = outBuf.toString() + errBuf.toString();
        assertTrue(combined.contains("[WARN]"),
                "Should warn that no previous release was found");
    }

    // -----------------------------------------------------------------------
    // --env-set
    // -----------------------------------------------------------------------

    @Test
    void envSet_withKeyValue_appendsToEnvFile() throws Exception {
        ProjectConfig config = new ProjectConfig();
        config.setProjectname("my-app");

        HashMap<String, String> args = new HashMap<>();
        args.put("--env-set", "API_KEY=abc123");

        registry.find("--env-set").execute(session, args, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(execChannel).setCommand(captor.capture());
        String cmd = captor.getValue();
        assertTrue(cmd.contains("API_KEY=abc123"), "env-set should include the key=value pair");
        assertTrue(cmd.contains(".env"), "env-set should target the .env file");
        assertTrue(cmd.contains("tee"), "env-set should append with tee");
    }

    // -----------------------------------------------------------------------
    // --backup
    // -----------------------------------------------------------------------

    @Test
    void backup_createsTarOnRemote() throws Exception {
        when(session.openChannel("sftp")).thenReturn(sftpChannel);

        ProjectConfig config = new ProjectConfig();
        config.setProjectname("my-app");

        HashMap<String, String> args = new HashMap<>();
        registry.find("--backup").execute(session, args, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(execChannel, atLeastOnce()).setCommand(captor.capture());
        boolean hasTar = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("tar") && c.contains("my-app"));
        assertTrue(hasTar, "--backup should create a tar archive of the project");
    }
}
