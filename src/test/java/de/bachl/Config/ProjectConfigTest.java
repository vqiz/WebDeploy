/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.Config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectConfigTest {

    @Test
    void defaultConstructor_createsObject() {
        ProjectConfig config = new ProjectConfig();
        assertNotNull(config);
    }

    @Test
    void fullConstructor_setsAllFields() {
        ProjectConfig config = new ProjectConfig(
                "myserver", "myproject", true, "/backend",
                true, "example.com", "npm run build", "dist",
                "mvn package", "target/app.jar", "/opt/app",
                "java -jar app.jar", "myservice", "/api", "http://localhost:8080",
                "10M", "/path/to/server.js");

        assertEquals("myserver", config.getServername());
        assertEquals("myproject", config.getProjectname());
        assertTrue(config.isNeedsbackend());
        assertEquals("/backend", config.getBackendpath());
        assertTrue(config.isEnabledomain());
        assertEquals("example.com", config.getDomain());
        assertEquals("npm run build", config.getBuildCommand());
        assertEquals("dist", config.getUploadPath());
        assertEquals("mvn package", config.getBackendBuildCommand());
        assertEquals("target/app.jar", config.getBackendArtifactPath());
        assertEquals("/opt/app", config.getBackendDeployPath());
        assertEquals("java -jar app.jar", config.getBackendRunCommand());
        assertEquals("myservice", config.getBackendServiceName());
        assertEquals("/api", config.getBackendProxyPath());
        assertEquals("http://localhost:8080", config.getBackendProxyTarget());
        assertEquals("10M", config.getClientMaxBodySize());
        assertEquals("/path/to/server.js", config.getBackendFilePath());
    }

    @Test
    void setServername_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setServername("prod");
        assertEquals("prod", config.getServername());
    }

    @Test
    void setProjectname_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setProjectname("my-app");
        assertEquals("my-app", config.getProjectname());
    }

    @Test
    void needsbackend_defaultFalse_setterWorks() {
        ProjectConfig config = new ProjectConfig();
        assertFalse(config.isNeedsbackend());
        config.setNeedsbackend(true);
        assertTrue(config.isNeedsbackend());
    }

    @Test
    void enabledomain_defaultFalse_setterWorks() {
        ProjectConfig config = new ProjectConfig();
        assertFalse(config.isEnabledomain());
        config.setEnabledomain(true);
        assertTrue(config.isEnabledomain());
    }

    @Test
    void setDomain_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setDomain("example.com");
        assertEquals("example.com", config.getDomain());
    }

    @Test
    void setBuildCommand_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setBuildCommand("gradle build");
        assertEquals("gradle build", config.getBuildCommand());
    }

    @Test
    void setUploadPath_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setUploadPath("build/dist");
        assertEquals("build/dist", config.getUploadPath());
    }

    @Test
    void setBackendProxyPath_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setBackendProxyPath("/api");
        assertEquals("/api", config.getBackendProxyPath());
    }

    @Test
    void setBackendProxyTarget_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setBackendProxyTarget("http://127.0.0.1:3000");
        assertEquals("http://127.0.0.1:3000", config.getBackendProxyTarget());
    }

    @Test
    void setClientMaxBodySize_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setClientMaxBodySize("500M");
        assertEquals("500M", config.getClientMaxBodySize());
    }

    @Test
    void setBackendFilePath_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setBackendFilePath("/src/server.js");
        assertEquals("/src/server.js", config.getBackendFilePath());
    }

    @Test
    void setBackendServiceName_updatesField() {
        ProjectConfig config = new ProjectConfig();
        config.setBackendServiceName("my-api");
        assertEquals("my-api", config.getBackendServiceName());
    }
}
