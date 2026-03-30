/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands;

import de.bachl.Config.ProjectConfig;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
    }

    private Command stubCommand(String key, String description) {
        return new Command() {
            public String getCommand() { return key; }
            public String getDescription() { return description; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) {}
        };
    }

    @Test
    void register_addsCommandToRegistry() {
        registry.register(stubCommand("--test", "Test cmd"));
        assertNotNull(registry.find("--test"), "Registered command should be findable");
    }

    @Test
    void find_returnsRegisteredCommand() {
        Command cmd = stubCommand("--hello", "Hello command");
        registry.register(cmd);
        assertSame(cmd, registry.find("--hello"));
    }

    @Test
    void find_returnsNullForUnknownCommand() {
        assertNull(registry.find("--unknown"), "Unknown key should return null");
    }

    @Test
    void register_multipleCommands_allAreRetrievable() {
        registry.register(stubCommand("--cmd1", "First"));
        registry.register(stubCommand("--cmd2", "Second"));
        registry.register(stubCommand("--cmd3", "Third"));

        assertNotNull(registry.find("--cmd1"));
        assertNotNull(registry.find("--cmd2"));
        assertNotNull(registry.find("--cmd3"));
    }

    @Test
    void printHelp_doesNotThrow() {
        registry.register(stubCommand("--alpha", "Alpha description"));
        registry.register(stubCommand("--beta", "Beta description"));
        assertDoesNotThrow(() -> registry.printHelp());
    }

    @Test
    void printHelp_outputsCommandKeysAndDescriptions() {
        PrintStream original = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));
        try {
            registry.register(stubCommand("--mykey", "My description text"));
            registry.printHelp();
        } finally {
            System.setOut(original);
        }
        String output = capture.toString();
        assertTrue(output.contains("--mykey"), "Help output should contain the command key");
        assertTrue(output.contains("My description text"), "Help output should contain the description");
    }

    @Test
    void printHelp_withEmptyRegistry_doesNotThrow() {
        assertDoesNotThrow(() -> registry.printHelp());
    }

    @Test
    void register_overridesExistingKeyWithNewCommand() {
        Command first = stubCommand("--dup", "First");
        Command second = stubCommand("--dup", "Second");
        registry.register(first);
        registry.register(second);
        assertSame(second, registry.find("--dup"), "Second registration should override the first");
    }
}
