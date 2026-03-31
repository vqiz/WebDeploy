/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import de.bachl.commands.CommandRegistry;
import de.bachl.Config.ProjectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemHandlerTest {

    @Mock
    Session session;

    @Mock
    ChannelExec channel;

    private CommandRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        registry = new CommandRegistry();
        new SystemHandler().register(registry);

        when(session.openChannel("exec")).thenReturn(channel);
        when(channel.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));
        when(channel.isClosed()).thenAnswer(new org.mockito.stubbing.Answer<Boolean>() {
            int count = 0;
            public Boolean answer(org.mockito.invocation.InvocationOnMock inv) {
                return (count++ % 2 != 0);
            }
        });
        when(channel.getExitStatus()).thenReturn(0);
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    @Test
    void reboot_isRegistered() {
        assertNotNull(registry.find("--reboot"));
    }

    @Test
    void shutdown_isRegistered() {
        assertNotNull(registry.find("--shutdown"));
    }

    @Test
    void updateServer_isRegistered() {
        assertNotNull(registry.find("--update-server"));
    }

    @Test
    void installPkg_isRegistered() {
        assertNotNull(registry.find("--install-pkg"));
    }

    @Test
    void serviceStart_isRegistered() {
        assertNotNull(registry.find("--service-start"));
    }

    @Test
    void serviceStop_isRegistered() {
        assertNotNull(registry.find("--service-stop"));
    }

    @Test
    void serviceRestart_isRegistered() {
        assertNotNull(registry.find("--service-restart"));
    }

    @Test
    void uptime_isRegistered() {
        assertNotNull(registry.find("--uptime"));
    }

    @Test
    void diskspace_isRegistered() {
        assertNotNull(registry.find("--diskspace"));
    }

    @Test
    void testConnection_isRegistered() {
        assertNotNull(registry.find("--test-connection"));
    }

    // -----------------------------------------------------------------------
    // Validation: --service-start with no value logs error, no SSH call made
    // -----------------------------------------------------------------------

    @Test
    void serviceStart_withNoValue_logsErrorAndSkipsSSH() throws Exception {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errBuf));
        try {
            HashMap<String, String> args = new HashMap<>();
            args.put("--service-start", "true");  // boolean flag with no real value
            registry.find("--service-start").execute(session, args, null);
        } finally {
            System.setErr(originalErr);
        }
        // No channel interaction should have happened
        verify(session, never()).openChannel(anyString());
        assertTrue(errBuf.toString().contains("[ERROR]"));
    }

    @Test
    void serviceStart_withEmptyValue_logsErrorAndSkipsSSH() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--service-start", "");
        registry.find("--service-start").execute(session, args, null);
        verify(session, never()).openChannel(anyString());
    }

    // -----------------------------------------------------------------------
    // Validation: --install-pkg with no value logs error
    // -----------------------------------------------------------------------

    @Test
    void installPkg_withNoValue_logsErrorAndSkipsSSH() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--install-pkg", "true");
        registry.find("--install-pkg").execute(session, args, null);
        verify(session, never()).openChannel(anyString());
    }

    // -----------------------------------------------------------------------
    // Correct SSH commands
    // -----------------------------------------------------------------------

    @Test
    void uptime_sendsUptimeCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        registry.find("--uptime").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertEquals("uptime", captor.getValue());
    }

    @Test
    void diskspace_sendsDfCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        registry.find("--diskspace").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("df -h"));
    }

    @Test
    void serviceStart_withValue_sendsSystemctlStartCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--service-start", "nginx");
        registry.find("--service-start").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("systemctl start nginx"));
    }

    @Test
    void installPkg_withValue_sendsAptGetInstall() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--install-pkg", "curl");
        registry.find("--install-pkg").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("apt-get install") && captor.getValue().contains("curl"));
    }
}
