/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.Config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileWriterTest {

    @TempDir
    Path tempDir;

    private final FileWriter fileWriter = new FileWriter();

    @Test
    void writeFile_createsFileWithContent() throws IOException {
        Path target = tempDir.resolve("test.txt");
        fileWriter.writeFile(target.toString(), "hello world");
        assertTrue(Files.exists(target), "File should exist after write");
        assertEquals("hello world", Files.readString(target));
    }

    @Test
    void readFile_returnsFileContent() throws IOException {
        Path target = tempDir.resolve("read.txt");
        Files.writeString(target, "read me");
        String content = fileWriter.readFile(target.toString());
        assertEquals("read me", content);
    }

    @Test
    void writeFile_createsParentDirectoriesIfNeeded() throws IOException {
        Path target = tempDir.resolve("nested/deep/dir/file.txt");
        fileWriter.writeFile(target.toString(), "nested content");
        assertTrue(Files.exists(target), "File in nested directories should be created");
        assertEquals("nested content", Files.readString(target));
    }

    @Test
    void readFile_returnsNullForNonExistentFile() {
        String result = fileWriter.readFile(tempDir.resolve("nonexistent.txt").toString());
        assertNull(result, "readFile should return null for a missing file");
    }

    @Test
    void writeFile_overwritesExistingContent() throws IOException {
        Path target = tempDir.resolve("overwrite.txt");
        fileWriter.writeFile(target.toString(), "original");
        fileWriter.writeFile(target.toString(), "updated");
        assertEquals("updated", Files.readString(target));
    }

    @Test
    void readFile_returnsExactBytes() throws IOException {
        Path target = tempDir.resolve("exact.txt");
        String content = "line1\nline2\n";
        Files.writeString(target, content);
        assertEquals(content, fileWriter.readFile(target.toString()));
    }
}
