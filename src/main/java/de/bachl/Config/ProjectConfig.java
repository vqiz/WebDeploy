/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.Config;

public class ProjectConfig {

    private String servername;
    private String projectname;
    private boolean needsbackend;
    private String backendpath;
    private boolean enabledomain;
    private String domain;

    private String buildCommand;
    private String uploadPath;

    private String backendBuildCommand;
    private String backendArtifactPath;
    private String backendDeployPath;
    private String backendRunCommand;
    private String backendServiceName;
    private String backendProxyPath;
    private String backendProxyTarget;
    private String clientMaxBodySize;
    private String backendFilePath;

    public ProjectConfig() {
    }

    public ProjectConfig(String servername, String projectname, boolean needsbackend, String backendpath,
            boolean enabledomain, String domain, String buildCommand, String uploadPath,
            String backendBuildCommand, String backendArtifactPath, String backendDeployPath,
            String backendRunCommand, String backendServiceName, String backendProxyPath, String backendProxyTarget,
            String clientMaxBodySize, String backendFilePath) {
        this.servername = servername;
        this.projectname = projectname;
        this.needsbackend = needsbackend;
        this.backendpath = backendpath;
        this.enabledomain = enabledomain;
        this.domain = domain;
        this.buildCommand = buildCommand;
        this.uploadPath = uploadPath;
        this.backendBuildCommand = backendBuildCommand;
        this.backendArtifactPath = backendArtifactPath;
        this.backendDeployPath = backendDeployPath;
        this.backendRunCommand = backendRunCommand;
        this.backendServiceName = backendServiceName;
        this.backendProxyPath = backendProxyPath;
        this.backendProxyTarget = backendProxyTarget;
        this.clientMaxBodySize = clientMaxBodySize;
        this.backendFilePath = backendFilePath;
    }

    public String getBackendBuildCommand() {
        return backendBuildCommand;
    }

    public void setBackendBuildCommand(String backendBuildCommand) {
        this.backendBuildCommand = backendBuildCommand;
    }

    public String getBackendArtifactPath() {
        return backendArtifactPath;
    }

    public void setBackendArtifactPath(String backendArtifactPath) {
        this.backendArtifactPath = backendArtifactPath;
    }

    public String getBackendDeployPath() {
        return backendDeployPath;
    }

    public void setBackendDeployPath(String backendDeployPath) {
        this.backendDeployPath = backendDeployPath;
    }

    public String getBackendRunCommand() {
        return backendRunCommand;
    }

    public void setBackendRunCommand(String backendRunCommand) {
        this.backendRunCommand = backendRunCommand;
    }

    public String getBackendServiceName() {
        return backendServiceName;
    }

    public void setBackendServiceName(String backendServiceName) {
        this.backendServiceName = backendServiceName;
    }

    public String getBackendProxyPath() {
        return backendProxyPath;
    }

    public void setBackendProxyPath(String backendProxyPath) {
        this.backendProxyPath = backendProxyPath;
    }

    public String getBackendProxyTarget() {
        return backendProxyTarget;
    }

    public void setBackendProxyTarget(String backendProxyTarget) {
        this.backendProxyTarget = backendProxyTarget;
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

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getClientMaxBodySize() {
        return clientMaxBodySize;
    }

    public void setClientMaxBodySize(String clientMaxBodySize) {
        this.clientMaxBodySize = clientMaxBodySize;
    }

    public String getBackendFilePath() {
        return backendFilePath;
    }

    public void setBackendFilePath(String backendFilePath) {
        this.backendFilePath = backendFilePath;
    }
}
