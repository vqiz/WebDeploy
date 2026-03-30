/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import de.bachl.Config.ConfigProvider;
import de.bachl.Config.ProjectConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EditProjectHandler reads webdeploy.config from user.dir and writes it back.
 * We redirect both user.dir and user.home to temp directories.
 */
class EditProjectHandlerTest {

    @TempDir
    Path tempHome;

    @TempDir
    Path tempDir;

    private String prevHome;
    private String prevDir;

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outBuf;
    private ByteArrayOutputStream errBuf;

    @BeforeEach
    void setUp() {
        prevHome = System.getProperty("user.home");
        prevDir = System.getProperty("user.dir");
        System.setProperty("user.home", tempHome.toString());
        System.setProperty("user.dir", tempDir.toString());

        outBuf = new ByteArrayOutputStream();
        errBuf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuf));
        System.setErr(new PrintStream(errBuf));
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", prevHome);
        System.setProperty("user.dir", prevDir);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private ProjectConfig writeConfig(String server, String project) {
        ProjectConfig config = new ProjectConfig();
        config.setServername(server);
        config.setProjectname(project);
        new ConfigProvider().setupProject(config);
        return config;
    }

    // -----------------------------------------------------------------------
    // --server update
    // -----------------------------------------------------------------------

    @Test
    void server_flag_updatesServername() {
        writeConfig("old-server", "app");

        HashMap<String, String> args = new HashMap<>();
        args.put("--editproject", "true");
        args.put("--server", "new-server");

        new EditProjectHandler().handle(args);

        ProjectConfig loaded = new ConfigProvider().getProjectConfig();
        assertEquals("new-server", loaded.getServername());
    }

    // -----------------------------------------------------------------------
    // --domain update
    // -----------------------------------------------------------------------

    @Test
    void domain_flag_updatesDomainAndEnablesIt() {
        writeConfig("server", "app");

        HashMap<String, String> args = new HashMap<>();
        args.put("--editproject", "true");
        args.put("--domain", "example.com");

        new EditProjectHandler().handle(args);

        ProjectConfig loaded = new ConfigProvider().getProjectConfig();
        assertEquals("example.com", loaded.getDomain());
        assertTrue(loaded.isEnabledomain(), "--domain should auto-enable enabledomain");
    }

    // -----------------------------------------------------------------------
    // --build-command update
    // -----------------------------------------------------------------------

    @Test
    void buildCommand_flag_updatesBuildCommand() {
        writeConfig("server", "app");

        HashMap<String, String> args = new HashMap<>();
        args.put("--editproject", "true");
        args.put("--build-command", "gradle assemble");

        new EditProjectHandler().handle(args);

        ProjectConfig loaded = new ConfigProvider().getProjectConfig();
        assertEquals("gradle assemble", loaded.getBuildCommand());
    }

    // -----------------------------------------------------------------------
    // No flags — warns user
    // -----------------------------------------------------------------------

    @Test
    void noFlags_warnsUser() {
        writeConfig("server", "app");

        HashMap<String, String> args = new HashMap<>();
        args.put("--editproject", "true");
        // No edit flags

        new EditProjectHandler().handle(args);

        String output = outBuf.toString();
        assertTrue(output.contains("[WARN]"),
                "Should warn when no edit flags are provided");
    }

    // -----------------------------------------------------------------------
    // No webdeploy.config — logs error
    // -----------------------------------------------------------------------

    @Test
    void missingConfig_logsError() {
        // No webdeploy.config exists in tempDir
        HashMap<String, String> args = new HashMap<>();
        args.put("--editproject", "true");
        args.put("--server", "something");

        new EditProjectHandler().handle(args);

        assertTrue(errBuf.toString().contains("[ERROR]"),
                "Should log error when no project config is found");
    }

    // -----------------------------------------------------------------------
    // --upload-path update
    // -----------------------------------------------------------------------

    @Test
    void uploadPath_flag_updatesUploadPath() {
        writeConfig("server", "app");

        HashMap<String, String> args = new HashMap<>();
        args.put("--editproject", "true");
        args.put("--upload-path", "build/static");

        new EditProjectHandler().handle(args);

        ProjectConfig loaded = new ConfigProvider().getProjectConfig();
        assertEquals("build/static", loaded.getUploadPath());
    }
}
