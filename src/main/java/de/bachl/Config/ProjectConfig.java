package de.bachl.Config;

public class ProjectConfig {

    private String servername;
    private String projectname;
    private boolean needsbackend;
    private String backendpath;
    private boolean enabledomain;
    private String domain;

    public ProjectConfig() {
    }

    public ProjectConfig(String servername, String projectname, boolean needsbackend, String backendpath,
            boolean enabledomain, String domain) {
        this.servername = servername;
        this.projectname = projectname;
        this.needsbackend = needsbackend;
        this.backendpath = backendpath;
        this.enabledomain = enabledomain;
        this.domain = domain;
    }

    public String getServername() {
        return servername;
    }

    public void setServername(String servername) {
        this.servername = servername;
    }

    public String getProjectname() {
        return projectname;
    }

    public void setProjectname(String projectname) {
        this.projectname = projectname;
    }

    public boolean isNeedsbackend() {
        return needsbackend;
    }

    public void setNeedsbackend(boolean needsbackend) {
        this.needsbackend = needsbackend;
    }

    public String getBackendpath() {
        return backendpath;
    }

    public void setBackendpath(String backendpath) {
        this.backendpath = backendpath;
    }

    public boolean isEnabledomain() {
        return enabledomain;
    }

    public void setEnabledomain(boolean enabledomain) {
        this.enabledomain = enabledomain;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
