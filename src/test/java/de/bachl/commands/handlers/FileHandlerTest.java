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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileHandlerTest {

    @Mock
    Session session;

    @Mock
    ChannelExec channel;

    private CommandRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        registry = new CommandRegistry();
        new FileHandler().register(registry);

        when(session.openChannel("exec")).thenReturn(channel);
        when(channel.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));
        when(channel.isClosed()).thenReturn(false, true);
        when(channel.getExitStatus()).thenReturn(0);
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    @Test
    void zipRemote_isRegistered() {
        assertNotNull(registry.find("--zip-remote"));
    }

    @Test
    void unzipRemote_isRegistered() {
        assertNotNull(registry.find("--unzip-remote"));
    }

    @Test
    void mv_isRegistered() {
        assertNotNull(registry.find("--mv"));
    }

    @Test
    void cp_isRegistered() {
        assertNotNull(registry.find("--cp"));
    }

    @Test
    void chown_isRegistered() {
        assertNotNull(registry.find("--chown"));
    }

    @Test
    void chmod_isRegistered() {
        assertNotNull(registry.find("--chmod"));
    }

    // -----------------------------------------------------------------------
    // Correct commands
    // -----------------------------------------------------------------------

    @Test
    void zipRemote_sendsZipCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--zip-remote", "/var/www/html/app");
        registry.find("--zip-remote").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("zip") && captor.getValue().contains("/var/www/html/app"));
    }

    @Test
    void mv_sendsMovCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--mv", "/tmp/a /tmp/b");
        registry.find("--mv").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().startsWith("mv "));
    }

    @Test
    void chown_sendsSudoChownCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--chown", "www-data:www-data /var/www/html");
        registry.find("--chown").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("sudo chown -R"));
    }
}
