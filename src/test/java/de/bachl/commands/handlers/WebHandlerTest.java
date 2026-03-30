/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import com.jcraft.jsch.ChannelExec;
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
class WebHandlerTest {

    @Mock
    Session session;

    @Mock
    ChannelExec channel;

    private CommandRegistry registry;

    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream errBuf;

    @BeforeEach
    void setUp() throws Exception {
        registry = new CommandRegistry();
        new WebHandler().register(registry);

        when(session.openChannel("exec")).thenReturn(channel);
        when(channel.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));
        when(channel.isClosed()).thenAnswer(new org.mockito.stubbing.Answer<Boolean>() {
            int count = 0;
            public Boolean answer(org.mockito.invocation.InvocationOnMock inv) {
                return (count++ % 2 != 0);
            }
        });
        when(channel.getExitStatus()).thenReturn(0);

        errBuf = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errBuf));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    @Test
    void restart_isRegistered() {
        assertNotNull(registry.find("--restart"));
    }

    @Test
    void status_isRegistered() {
        assertNotNull(registry.find("--status"));
    }

    @Test
    void logs_isRegistered() {
        assertNotNull(registry.find("--logs"));
    }

    @Test
    void accessLogs_isRegistered() {
        assertNotNull(registry.find("--access-logs"));
    }

    @Test
    void maintenance_isRegistered() {
        assertNotNull(registry.find("--maintenance"));
    }

    @Test
    void clearCache_isRegistered() {
        assertNotNull(registry.find("--clear-cache"));
    }

    // -----------------------------------------------------------------------
    // Correct SSH commands
    // -----------------------------------------------------------------------

    @Test
    void restart_sendsSystemctlRestartNginx() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        registry.find("--restart").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("systemctl restart nginx"));
    }

    @Test
    void logs_tailsNginxErrorLog() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        registry.find("--logs").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("nginx/error.log"));
    }

    // -----------------------------------------------------------------------
    // --maintenance toggle
    // -----------------------------------------------------------------------

    @Test
    void maintenance_withNullConfig_logsError() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        registry.find("--maintenance").execute(session, args, null);  // null config
        assertTrue(errBuf.toString().contains("[ERROR]"),
                "Should log error when project config is null");
        verify(session, never()).openChannel(anyString());
    }

    @Test
    void maintenance_whenMaintenanceFileExists_removesIt() throws Exception {
        // First sendCommand (ls check) succeeds → file exists → should rm it
        ProjectConfig config = new ProjectConfig();
        config.setProjectname("my-app");

        HashMap<String, String> args = new HashMap<>();
        registry.find("--maintenance").execute(session, args, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        // The last meaningful command should be rm (disable maintenance)
        boolean hasRm = captor.getAllValues().stream()
                .anyMatch(c -> c.startsWith("rm ") && c.contains("maintenance.html"));
        assertTrue(hasRm, "Should remove maintenance.html when it exists");
    }

    @Test
    void maintenance_whenMaintenanceFileAbsent_createsIt() throws Exception {
        // Make the ls check fail (non-zero exit) so file is considered absent
        when(channel.getExitStatus()).thenReturn(2, 0); // ls fails, echo succeeds

        ProjectConfig config = new ProjectConfig();
        config.setProjectname("my-app");

        HashMap<String, String> args = new HashMap<>();
        registry.find("--maintenance").execute(session, args, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        boolean hasEcho = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("maintenance.html") && c.contains("echo"));
        assertTrue(hasEcho, "Should create maintenance.html when it does not exist");
    }
}
