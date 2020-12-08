package com.cx.plugin.cli.configascode;

import com.typesafe.config.Optional;

public class ProjectConfig {
    @Optional
    private String fullPath;
    @Optional
    private String origin;

    public ProjectConfig() {
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }
}
