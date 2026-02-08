/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.Config;

public class Config {
    public String keypath;
    public String name;
    public String host;
    public String user;
    public String password;
    public String buildCommand = "npm run build";

    public Config(String keypath, String name, String host, String user, String password) {
        this.keypath = keypath;
        this.name = name;
        this.host = host;
        this.user = user;
        this.password = password;
    }

    public String getKeypath() {
        return keypath;
    }

    public void setKeypath(String keypath) {
        this.keypath = keypath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand;
    }
}
