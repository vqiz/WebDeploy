/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.services;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SSLServiceTest {

    @Mock
    Session session;

    @Mock
    ChannelExec channel;

    private void stubUnlimitedChannelCalls() throws Exception {
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
    // installCertbot
    // -----------------------------------------------------------------------

    @Test
    void installCertbot_installsSnapd() throws Exception {
        stubUnlimitedChannelCalls();
        new SSLService().installCertbot(session);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c -> c.contains("snapd")),
                "installCertbot should install snapd");
    }

    @Test
    void installCertbot_installsClassicCertbot() throws Exception {
        stubUnlimitedChannelCalls();
        new SSLService().installCertbot(session);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c -> c.contains("certbot") && c.contains("--classic")),
                "installCertbot should run snap install --classic certbot");
    }

    @Test
    void installCertbot_createsSymlink() throws Exception {
        stubUnlimitedChannelCalls();
        new SSLService().installCertbot(session);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c -> c.contains("ln") && c.contains("certbot")),
                "installCertbot should create a certbot symlink");
    }

    @Test
    void installCertbot_runsAptGetUpdate() throws Exception {
        stubUnlimitedChannelCalls();
        new SSLService().installCertbot(session);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c -> c.contains("apt-get update")),
                "installCertbot should run apt-get update first");
    }

    // -----------------------------------------------------------------------
    // setupSSL
    // -----------------------------------------------------------------------

    @Test
    void setupSSL_runsCertbotWithNginxFlag() throws Exception {
        stubUnlimitedChannelCalls();
        new SSLService().setupSSL(session, "example.com");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c -> c.contains("certbot") && c.contains("--nginx")),
                "setupSSL should run certbot --nginx");
    }

    @Test
    void setupSSL_includesDomainInCommand() throws Exception {
        stubUnlimitedChannelCalls();
        new SSLService().setupSSL(session, "my-site.com");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c -> c.contains("-d my-site.com")),
                "setupSSL command should include the domain with -d flag");
    }

    @Test
    void setupSSL_usesNonInteractiveFlags() throws Exception {
        stubUnlimitedChannelCalls();
        new SSLService().setupSSL(session, "quiet.com");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c ->
                c.contains("--non-interactive") && c.contains("--agree-tos")),
                "setupSSL should use non-interactive flags for automation");
    }

    @Test
    void setupSSL_throwsExceptionOnNonZeroExitCode() throws Exception {
        when(session.openChannel("exec")).thenReturn(channel);
        when(channel.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(channel.isClosed()).thenReturn(false, true);
        when(channel.getExitStatus()).thenReturn(1);

        assertThrows(Exception.class,
                () -> new SSLService().setupSSL(session, "fail.com"),
                "SSLService should throw when certbot command fails");
    }
}
