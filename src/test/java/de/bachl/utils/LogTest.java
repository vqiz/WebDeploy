/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class LogTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private ByteArrayOutputStream outCapture;
    private ByteArrayOutputStream errCapture;

    @BeforeEach
    void redirectStreams() {
        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outCapture));
        System.setErr(new PrintStream(errCapture));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void info_writesToStdout() {
        Log.info("hello info");
        String output = outCapture.toString();
        assertTrue(output.contains("hello info"), "stdout should contain the info message");
        assertTrue(output.contains("[INFO]"), "stdout should contain the [INFO] tag");
    }

    @Test
    void warn_writesToStdout() {
        Log.warn("watch out");
        String output = outCapture.toString();
        assertTrue(output.contains("watch out"), "stdout should contain the warn message");
        assertTrue(output.contains("[WARN]"), "stdout should contain the [WARN] tag");
    }

    @Test
    void error_writesToStderr() {
        Log.error("something failed");
        String output = errCapture.toString();
        assertTrue(output.contains("something failed"), "stderr should contain the error message");
        assertTrue(output.contains("[ERROR]"), "stderr should contain the [ERROR] tag");
    }

    @Test
    void error_doesNotWriteToStdout() {
        Log.error("error msg");
        assertTrue(outCapture.toString().isEmpty(), "stdout should remain empty for error messages");
    }

    @Test
    void success_writesToStdout() {
        Log.success("all good");
        String output = outCapture.toString();
        assertTrue(output.contains("all good"), "stdout should contain the success message");
        assertTrue(output.contains("[OK]"), "stdout should contain the [OK] tag");
    }

    @Test
    void info_doesNotWriteToStderr() {
        Log.info("info only");
        assertTrue(errCapture.toString().isEmpty(), "stderr should remain empty for info messages");
    }
}
