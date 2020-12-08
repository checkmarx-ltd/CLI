package com.cx.plugin.cli.configascode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.typesafe.config.Optional;

public class ConfigAsCode {
    @Optional
    private ProjectConfig project;
    @Optional
    private SastConfig sast;
    @Optional
    private ScaConfig sca;

    public ConfigAsCode() {
    }

    public SastConfig getSast() {
        return sast;
    }

    public void setSast(SastConfig sast) {
        this.sast = sast;
    }

    public ProjectConfig getProject() {
        return project;
    }

    public void setProject(ProjectConfig project) {
        this.project = project;
    }

    public ScaConfig getSca() {
        return sca;
    }

    public void setSca(ScaConfig sca) {
        this.sca = sca;
    }
}
