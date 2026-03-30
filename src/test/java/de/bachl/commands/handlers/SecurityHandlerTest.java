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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityHandlerTest {

    @Mock
    Session session;

    @Mock
    ChannelExec channel;

    private CommandRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        registry = new CommandRegistry();
        new SecurityHandler().register(registry);

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
    void firewallStatus_isRegistered() {
        assertNotNull(registry.find("--firewall-status"));
    }

    @Test
    void firewallAllow_isRegistered() {
        assertNotNull(registry.find("--firewall-allow"));
    }

    @Test
    void firewallDeny_isRegistered() {
        assertNotNull(registry.find("--firewall-deny"));
    }

    @Test
    void fail2banInstall_isRegistered() {
        assertNotNull(registry.find("--fail2ban-install"));
    }

    @Test
    void fail2banStatus_isRegistered() {
        assertNotNull(registry.find("--fail2ban-status"));
    }

    @Test
    void checkPorts_isRegistered() {
        assertNotNull(registry.find("--check-ports"));
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    @Test
    void firewallAllow_withNoPort_skipsSSH() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--firewall-allow", "true");
        registry.find("--firewall-allow").execute(session, args, null);
        verify(session, never()).openChannel(anyString());
    }

    @Test
    void firewallAllow_withEmptyPort_skipsSSH() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--firewall-allow", "");
        registry.find("--firewall-allow").execute(session, args, null);
        verify(session, never()).openChannel(anyString());
    }

    @Test
    void firewallDeny_withNoPort_skipsSSH() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--firewall-deny", "true");
        registry.find("--firewall-deny").execute(session, args, null);
        verify(session, never()).openChannel(anyString());
    }

    // -----------------------------------------------------------------------
    // Correct SSH commands
    // -----------------------------------------------------------------------

    @Test
    void firewallAllow_withPort_sendsUfwAllowCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--firewall-allow", "443");
        registry.find("--firewall-allow").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("ufw allow 443"),
                "Should send 'ufw allow 443'");
    }

    @Test
    void firewallDeny_withPort_sendsUfwDenyCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        args.put("--firewall-deny", "23");
        registry.find("--firewall-deny").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("ufw deny 23"),
                "Should send 'ufw deny 23'");
    }

    @Test
    void firewallStatus_sendsUfwStatusCommand() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        registry.find("--firewall-status").execute(session, args, null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("ufw status"));
    }

    @Test
    void fail2banInstall_sendsMultipleCommands() throws Exception {
        HashMap<String, String> args = new HashMap<>();
        registry.find("--fail2ban-install").execute(session, args, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, times(3)).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c -> c.contains("fail2ban")));
        assertTrue(cmds.stream().anyMatch(c -> c.contains("systemctl start fail2ban")));
        assertTrue(cmds.stream().anyMatch(c -> c.contains("systemctl enable fail2ban")));
    }
}
