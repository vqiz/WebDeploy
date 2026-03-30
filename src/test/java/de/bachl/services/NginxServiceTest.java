/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.services;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import de.bachl.Config.ProjectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NginxService does not expose the config string directly, so we capture the
 * commands sent through the mocked SSH channel to verify correctness.
 *
 * All SSH calls are fully mocked — no network connection is made.
 */
@ExtendWith(MockitoExtension.class)
class NginxServiceTest {

    @Mock
    Session session;

    @Mock
    ChannelExec channel;

    @BeforeEach
    void setUpChannel() throws Exception {
        when(session.openChannel("exec")).thenReturn(channel);
        when(channel.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(channel.isClosed()).thenReturn(false, true);
        when(channel.getExitStatus()).thenReturn(0);
    }

    private ProjectConfig basicConfig(String project, String domain) {
        ProjectConfig config = new ProjectConfig();
        config.setProjectname(project);
        config.setDomain(domain);
        return config;
    }

    // -----------------------------------------------------------------------
    // install
    // -----------------------------------------------------------------------

    @Test
    void install_sendsAptGetInstallNginx() throws Exception {
        // Each sendCommand call opens a channel, so reset the stubbing to return
        // fresh streams for unlimited calls.
        when(session.openChannel("exec")).thenReturn(channel);
        when(channel.getInputStream())
                .thenReturn(new ByteArrayInputStream(new byte[0]),
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayInputStream(new byte[0]));
        when(channel.isClosed())
                .thenReturn(false, true, false, true, false, true, false, true);
        when(channel.getExitStatus()).thenReturn(0);

        new NginxService().install(session);

        ArgumentCaptor<String> cmdCaptor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(cmdCaptor.capture());
        List<String> commands = cmdCaptor.getAllValues();

        assertTrue(commands.stream().anyMatch(c -> c.contains("apt-get install") && c.contains("nginx")),
                "install() should run apt-get install nginx");
        assertTrue(commands.stream().anyMatch(c -> c.contains("systemctl start nginx")),
                "install() should start nginx");
    }

    // -----------------------------------------------------------------------
    // setupSite
    // -----------------------------------------------------------------------

    @Test
    void setupSite_includesStaticLocationBlock_whenNoRootProxy() throws Exception {
        stubUnlimitedChannelCalls();

        ProjectConfig config = basicConfig("myapp", "example.com");
        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        // The echo command for the nginx config should contain try_files
        boolean hasStaticLocation = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("try_files"));
        assertTrue(hasStaticLocation, "Config should include static location / block when no root proxy");
    }

    @Test
    void setupSite_includesCorrectDomain() throws Exception {
        stubUnlimitedChannelCalls();

        ProjectConfig config = basicConfig("myapp", "my-domain.com");
        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        boolean hasDomain = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("my-domain.com"));
        assertTrue(hasDomain, "Nginx config should contain the project domain");
    }

    @Test
    void setupSite_includesProjectName() throws Exception {
        stubUnlimitedChannelCalls();

        ProjectConfig config = basicConfig("cool-project", "example.com");
        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        boolean hasProjectName = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("cool-project"));
        assertTrue(hasProjectName, "Nginx config should reference the project name");
    }

    @Test
    void setupSite_withBackendProxyPath_includesProxyPassBlock() throws Exception {
        stubUnlimitedChannelCalls();

        ProjectConfig config = basicConfig("api-app", "api.example.com");
        config.setBackendProxyPath("/api");
        config.setBackendProxyTarget("http://localhost:8080");

        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        boolean hasProxy = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("proxy_pass") && c.contains("http://localhost:8080"));
        assertTrue(hasProxy, "Config should include proxy_pass for backend proxy target");
    }

    @Test
    void setupSite_withRootProxy_excludesStaticLocationBlock() throws Exception {
        stubUnlimitedChannelCalls();

        ProjectConfig config = basicConfig("spa", "spa.example.com");
        config.setBackendProxyPath("/");
        config.setBackendProxyTarget("http://localhost:3000");

        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        // When root is proxied there should be no try_files (static location block)
        boolean hasStaticLocation = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("try_files $uri $uri/ /index.html"));
        assertFalse(hasStaticLocation,
                "Static location block should be omitted when root path is proxied");
    }

    @Test
    void setupSite_usesDefaultClientMaxBodySize_whenNotSet() throws Exception {
        stubUnlimitedChannelCalls();

        ProjectConfig config = basicConfig("nosize", "nosize.example.com");
        // clientMaxBodySize is not set

        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        boolean hasDefault = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("client_max_body_size 10M"));
        assertTrue(hasDefault, "Default client_max_body_size should be 10M");
    }

    @Test
    void setupSite_usesCustomClientMaxBodySize_whenSet() throws Exception {
        stubUnlimitedChannelCalls();

        ProjectConfig config = basicConfig("bigapp", "bigapp.com");
        config.setClientMaxBodySize("500M");

        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        boolean has500M = captor.getAllValues().stream()
                .anyMatch(c -> c.contains("client_max_body_size 500M"));
        assertTrue(has500M, "Custom client_max_body_size should be used");
    }

    // -----------------------------------------------------------------------
    // reload
    // -----------------------------------------------------------------------

    @Test
    void reload_sendsSystemctlReloadNginx() throws Exception {
        new NginxService().reload(session);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("systemctl reload nginx"),
                "reload() should run systemctl reload nginx");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Stubs the channel mock to handle an arbitrary number of sendCommand calls.
     */
    private void stubUnlimitedChannelCalls() throws Exception {
        when(channel.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));
        // Alternate: not-closed, closed for each invocation
        when(channel.isClosed()).thenAnswer(new org.mockito.stubbing.Answer<Boolean>() {
            int count = 0;
            public Boolean answer(org.mockito.invocation.InvocationOnMock inv) {
                return (count++ % 2 != 0);
            }
        });
        when(channel.getExitStatus()).thenReturn(0);
    }
}
