/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArgsLister reads from System.getProperty("user.home") + "/.webdeploy/servers/".
 * We override user.home for each test to point to a temp directory so we avoid
 * touching the real filesystem.
 */
class ArgsListerTest {

    @TempDir
    Path tempHome;

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outCapture;
    private ByteArrayOutputStream errCapture;

    private String previousHome;

    @BeforeEach
    void setUp() {
        previousHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outCapture));
        System.setErr(new PrintStream(errCapture));
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", previousHome);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void print_withNoServersDirectory_doesNotThrow() {
        // The directory does not exist in the fresh temp home — should warn, not throw
        assertDoesNotThrow(() -> new ArgsLister().print());
    }

    @Test
    void print_withNoServersDirectory_outputsWarning() {
        new ArgsLister().print();
        String combined = outCapture.toString() + errCapture.toString();
        // Log.warn goes to stdout
        assertTrue(combined.contains("[WARN]"), "Should print a warning when no servers dir exists");
    }

    @Test
    void print_withEmptyServersDirectory_outputsWarning() throws Exception {
        File serversDir = tempHome.resolve(".webdeploy/servers").toFile();
        serversDir.mkdirs();

        new ArgsLister().print();
        String combined = outCapture.toString() + errCapture.toString();
        assertTrue(combined.contains("[WARN]"), "Should warn when servers directory is empty");
    }

    @Test
    void print_withServers_listsServerNames() throws Exception {
        File serversDir = tempHome.resolve(".webdeploy/servers").toFile();
        serversDir.mkdirs();
        new File(serversDir, "production").createNewFile();
        new File(serversDir, "staging").createNewFile();

        new ArgsLister().print();
        String output = outCapture.toString();
        assertTrue(output.contains("production"), "Should list 'production' server");
        assertTrue(output.contains("staging"), "Should list 'staging' server");
    }

    @Test
    void print_withServers_doesNotListHiddenFiles() throws Exception {
        File serversDir = tempHome.resolve(".webdeploy/servers").toFile();
        serversDir.mkdirs();
        new File(serversDir, "myserver").createNewFile();
        new File(serversDir, ".hidden").createNewFile();

        new ArgsLister().print();
        String output = outCapture.toString();
        assertTrue(output.contains("myserver"), "Should list visible server");
        assertFalse(output.contains(".hidden"), "Should not list hidden files");
    }
}
