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
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NginxServiceTest {

    @Mock Session session;
    @Mock ChannelExec channel;

    @BeforeEach
    void setUp() throws Exception {
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

    private ProjectConfig basic(String project, String domain) {
        ProjectConfig c = new ProjectConfig();
        c.setProjectname(project);
        c.setDomain(domain);
        return c;
    }

    // -----------------------------------------------------------------------
    // buildNginxConfig — pure logic, no SSH
    // -----------------------------------------------------------------------

    @Test
    void buildNginxConfig_includesCorrectDomain() {
        ProjectConfig config = basic("myapp", "my-domain.com");
        String result = new NginxService().buildNginxConfig(config);
        assertTrue(result.contains("my-domain.com"), "Config should contain the domain");
    }

    @Test
    void buildNginxConfig_includesProjectName() {
        ProjectConfig config = basic("cool-project", "example.com");
        String result = new NginxService().buildNginxConfig(config);
        assertTrue(result.contains("cool-project"), "Config should contain project name");
    }

    @Test
    void buildNginxConfig_hasStaticLocationBlock_whenNoRootProxy() {
        ProjectConfig config = basic("myapp", "example.com");
        String result = new NginxService().buildNginxConfig(config);
        assertTrue(result.contains("try_files"), "Config should have try_files when no root proxy");
    }

    @Test
    void buildNginxConfig_noStaticLocation_whenRootProxied() {
        ProjectConfig config = basic("spa", "spa.example.com");
        config.setBackendProxyPath("/");
        config.setBackendProxyTarget("http://localhost:3000");
        String result = new NginxService().buildNginxConfig(config);
        assertFalse(result.contains("try_files $uri $uri/ /index.html"),
                "Static block should be omitted when root is proxied");
    }

    @Test
    void buildNginxConfig_includesProxyPass_whenBackendConfigured() {
        ProjectConfig config = basic("api-app", "api.example.com");
        config.setBackendProxyPath("/api");
        config.setBackendProxyTarget("http://localhost:8080");
        String result = new NginxService().buildNginxConfig(config);
        assertTrue(result.contains("proxy_pass"), "Config should include proxy_pass");
        assertTrue(result.contains("http://localhost:8080"), "Config should include backend target");
    }

    @Test
    void buildNginxConfig_usesDefaultClientMaxBodySize() {
        ProjectConfig config = basic("nosize", "nosize.example.com");
        String result = new NginxService().buildNginxConfig(config);
        assertTrue(result.contains("client_max_body_size 10M"), "Default size should be 10M");
    }

    @Test
    void buildNginxConfig_usesCustomClientMaxBodySize() {
        ProjectConfig config = basic("bigapp", "bigapp.com");
        config.setClientMaxBodySize("500M");
        String result = new NginxService().buildNginxConfig(config);
        assertTrue(result.contains("client_max_body_size 500M"), "Custom size should be used");
    }

    @Test
    void buildNginxConfig_fallsBackToUnderscore_whenDomainNull() {
        ProjectConfig config = basic("nodomain", null);
        String result = new NginxService().buildNginxConfig(config);
        assertTrue(result.contains("server_name _"), "Should fall back to _ when domain is null");
    }

    // -----------------------------------------------------------------------
    // install — SSH interaction
    // -----------------------------------------------------------------------

    @Test
    void install_sendsAptGetInstallNginx() throws Exception {
        new NginxService().install(session);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());
        List<String> cmds = captor.getAllValues();

        assertTrue(cmds.stream().anyMatch(c -> c.contains("apt-get install") && c.contains("nginx")),
                "install() should run apt-get install nginx");
        assertTrue(cmds.stream().anyMatch(c -> c.contains("systemctl start nginx")),
                "install() should start nginx");
    }

    // -----------------------------------------------------------------------
    // setupSite — verifies base64 write is sent
    // -----------------------------------------------------------------------

    @Test
    void setupSite_sendsBase64WriteCommand() throws Exception {
        ProjectConfig config = basic("myapp", "example.com");
        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        assertTrue(captor.getAllValues().stream().anyMatch(c -> c.contains("base64 -d") && c.contains("tee")),
                "setupSite() should write config via base64 decode + tee");
    }

    @Test
    void setupSite_enablesSite() throws Exception {
        ProjectConfig config = basic("testsite", "test.com");
        new NginxService().setupSite(session, config);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).setCommand(captor.capture());

        assertTrue(captor.getAllValues().stream().anyMatch(c -> c.contains("sites-enabled") && c.contains("testsite")),
                "setupSite() should create symlink in sites-enabled");
    }

    // -----------------------------------------------------------------------
    // reload
    // -----------------------------------------------------------------------

    @Test
    void reload_sendsSystemctlReloadNginx() throws Exception {
        new NginxService().reload(session);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(channel).setCommand(captor.capture());
        assertTrue(captor.getValue().contains("systemctl reload nginx"));
    }
}
