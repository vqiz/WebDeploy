package de.bachl.Config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectConfig {

    String servername;
    String projectname;

    boolean needsbackend;
    String backendpath;

    boolean enabledomain;
    String domain;

}
