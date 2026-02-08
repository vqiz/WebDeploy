/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import de.bachl.utils.Log;

public class FileWriter {

    public void writeFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Log.error("Failed to write to file: " + filePath + ". Error: " + e.getMessage());
        }
    }

    public String readFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                Log.error("File not found: " + filePath);
                return null;
            }
            return new String(Files.readAllBytes(path));
        } catch (IOException e) {
            Log.error("Failed to read file: " + filePath + ". Error: " + e.getMessage());
            return null;
        }
    }

}
