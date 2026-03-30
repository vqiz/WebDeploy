/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.Config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void constructor_setsAllFields() {
        Config config = new Config("/home/user/.ssh/id_rsa", "myserver", "1.2.3.4", "root", "secret");
        assertEquals("/home/user/.ssh/id_rsa", config.getKeypath());
        assertEquals("myserver", config.getName());
        assertEquals("1.2.3.4", config.getHost());
        assertEquals("root", config.getUser());
        assertEquals("secret", config.getPassword());
    }

    @Test
    void defaultBuildCommand_isNpmRunBuild() {
        Config config = new Config("/key", "name", "host", "user", "pass");
        assertEquals("npm run build", config.getBuildCommand());
    }

    @Test
    void setKeypath_updatesField() {
        Config config = new Config("/old", "name", "host", "user", "pass");
        config.setKeypath("/new/path");
        assertEquals("/new/path", config.getKeypath());
    }

    @Test
    void setName_updatesField() {
        Config config = new Config("/key", "old-name", "host", "user", "pass");
        config.setName("new-name");
        assertEquals("new-name", config.getName());
    }

    @Test
    void setHost_updatesField() {
        Config config = new Config("/key", "name", "old.host", "user", "pass");
        config.setHost("new.host");
        assertEquals("new.host", config.getHost());
    }

    @Test
    void setUser_updatesField() {
        Config config = new Config("/key", "name", "host", "old-user", "pass");
        config.setUser("new-user");
        assertEquals("new-user", config.getUser());
    }

    @Test
    void setPassword_updatesField() {
        Config config = new Config("/key", "name", "host", "user", "old-pass");
        config.setPassword("new-pass");
        assertEquals("new-pass", config.getPassword());
    }

    @Test
    void setBuildCommand_updatesField() {
        Config config = new Config("/key", "name", "host", "user", "pass");
        config.setBuildCommand("mvn package");
        assertEquals("mvn package", config.getBuildCommand());
    }
}
