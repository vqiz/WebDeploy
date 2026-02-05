package de.bachl.Config;

import lombok.Data;

@Data

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
}
