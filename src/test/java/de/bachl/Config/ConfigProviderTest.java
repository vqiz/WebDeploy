/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigProvider uses System.getProperty("user.home") for server configs and
 * System.getProperty("user.dir") for webdeploy.config. Both are overridden per
 * test so we never touch the real filesystem.
 */
class ConfigProviderTest {

    @TempDir
    Path tempHome;

    @TempDir
    Path tempDir;

    private String previousHome;
    private String previousDir;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @BeforeEach
    void setUp() {
        previousHome = System.getProperty("user.home");
        previousDir = System.getProperty("user.dir");
        System.setProperty("user.home", tempHome.toString());
        System.setProperty("user.dir", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", previousHome);
        System.setProperty("user.dir", previousDir);
    }

    // -----------------------------------------------------------------------
    // setupServer / getServerConfig
    // -----------------------------------------------------------------------

    @Test
    void setupServer_writesJsonFileInServersDirectory() throws IOException {
        Config config = new Config("/home/user/.ssh/id_rsa", "prod", "1.2.3.4", "root", "secret");
        new ConfigProvider().setupServer(config);

        File serverFile = tempHome.resolve(".webdeploy/servers/prod").toFile();
        assertTrue(serverFile.exists(), "Server config file should be created");
        String json = Files.readString(serverFile.toPath());
        assertTrue(json.contains("prod"), "JSON should contain server name");
        assertTrue(json.contains("1.2.3.4"), "JSON should contain host");
    }

    @Test
    void getServerConfig_loadsWrittenConfig() {
        Config original = new Config("/home/user/.ssh/id_rsa", "staging", "9.8.7.6", "deploy", "pw123");
        ConfigProvider provider = new ConfigProvider();
        provider.setupServer(original);

        Config loaded = provider.getServerConfig("staging");
        assertNotNull(loaded);
        assertEquals("staging", loaded.getName());
        assertEquals("9.8.7.6", loaded.getHost());
        assertEquals("deploy", loaded.getUser());
        assertEquals("pw123", loaded.getPassword());
    }

    @Test
    void getServerConfig_roundtripsKeypath() {
        Config original = new Config("/root/.ssh/id_ed25519", "keytest", "5.5.5.5", "admin", "");
        ConfigProvider provider = new ConfigProvider();
        provider.setupServer(original);

        Config loaded = provider.getServerConfig("keytest");
        assertEquals("/root/.ssh/id_ed25519", loaded.getKeypath());
    }

    // -----------------------------------------------------------------------
    // setupProject / getProjectConfig
    // -----------------------------------------------------------------------

    @Test
    void setupProject_writesWebdeployConfigInCurrentDir() throws IOException {
        ProjectConfig projectConfig = new ProjectConfig();
        projectConfig.setProjectname("my-app");
        projectConfig.setServername("prod");

        new ConfigProvider().setupProject(projectConfig);

        File configFile = tempDir.resolve("webdeploy.config").toFile();
        assertTrue(configFile.exists(), "webdeploy.config should be written to user.dir");
    }

    @Test
    void getProjectConfig_loadsWrittenProjectConfig() {
        ProjectConfig original = new ProjectConfig();
        original.setProjectname("web-app");
        original.setServername("myserver");
        original.setBuildCommand("npm run build");

        ConfigProvider provider = new ConfigProvider();
        provider.setupProject(original);

        ProjectConfig loaded = provider.getProjectConfig();
        assertNotNull(loaded);
        assertEquals("web-app", loaded.getProjectname());
        assertEquals("myserver", loaded.getServername());
        assertEquals("npm run build", loaded.getBuildCommand());
    }

    @Test
    void getProjectConfig_returnsNullWhenNoConfigFile() {
        // No webdeploy.config has been written in tempDir
        ProjectConfig result = new ConfigProvider().getProjectConfig();
        assertNull(result, "getProjectConfig should return null when webdeploy.config is absent");
    }

    @Test
    void setupProject_preservesBooleanFields() {
        ProjectConfig original = new ProjectConfig();
        original.setProjectname("bool-test");
        original.setServername("s");
        original.setNeedsbackend(true);
        original.setEnabledomain(true);
        original.setDomain("example.com");

        ConfigProvider provider = new ConfigProvider();
        provider.setupProject(original);

        ProjectConfig loaded = provider.getProjectConfig();
        assertTrue(loaded.isNeedsbackend());
        assertTrue(loaded.isEnabledomain());
        assertEquals("example.com", loaded.getDomain());
    }
}
