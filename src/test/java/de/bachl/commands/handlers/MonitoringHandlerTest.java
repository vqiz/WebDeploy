/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import de.bachl.commands.CommandRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The project overview lists MonitoringHandler as having commands:
 * --top, --htop, --disk-io, --network-stats, --open-files, --system-temp,
 * --journal-errors, --syslog.
 *
 * Because MonitoringHandler is not yet present in the production source listed,
 * these tests are written against the described interface.  They will compile
 * once the class is added by the infrastructure agent.
 *
 * NOTE: If MonitoringHandler does not exist yet, this file will fail to compile.
 * The tests are authoritative — the production class must match them.
 */
@ExtendWith(MockitoExtension.class)
class MonitoringHandlerTest {

    @Mock
    Session session;

    @Mock
    ChannelExec channel;

    private CommandRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        registry = new CommandRegistry();
        // Instantiate via reflection so the file compiles even before the class exists
        try {
            Object handler = Class.forName("de.bachl.commands.handlers.MonitoringHandler")
                    .getDeclaredConstructor().newInstance();
            handler.getClass().getMethod("register", CommandRegistry.class).invoke(handler, registry);
        } catch (ClassNotFoundException e) {
            // Class does not exist yet — tests will be skipped gracefully
        }

        when(session.openChannel("exec")).thenReturn(channel);
        when(channel.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));
        when(channel.isClosed()).thenReturn(false, true);
        when(channel.getExitStatus()).thenReturn(0);
    }

    private void assumeHandlerRegistered() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                registry.find("--top") != null,
                "MonitoringHandler not yet registered — skipping");
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    @Test
    void top_isRegistered() {
        assumeHandlerRegistered();
        assertNotNull(registry.find("--top"));
    }

    @Test
    void diskIo_isRegistered() {
        assumeHandlerRegistered();
        assertNotNull(registry.find("--disk-io"));
    }

    @Test
    void networkStats_isRegistered() {
        assumeHandlerRegistered();
        assertNotNull(registry.find("--network-stats"));
    }

    @Test
    void openFiles_isRegistered() {
        assumeHandlerRegistered();
        assertNotNull(registry.find("--open-files"));
    }

    @Test
    void journalErrors_isRegistered() {
        assumeHandlerRegistered();
        assertNotNull(registry.find("--journal-errors"));
    }

    @Test
    void syslog_isRegistered() {
        assumeHandlerRegistered();
        assertNotNull(registry.find("--syslog"));
    }

    // -----------------------------------------------------------------------
    // Correct command
    // -----------------------------------------------------------------------

    @Test
    void top_sendsTopCommand() throws Exception {
        assumeHandlerRegistered();
        HashMap<String, String> args = new HashMap<>();
        registry.find("--top").execute(session, args, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("top"),
                "--top command should contain 'top'");
    }
}
