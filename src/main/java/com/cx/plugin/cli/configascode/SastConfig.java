package com.cx.plugin.cli.configascode;

import com.typesafe.config.Optional;

public class SastConfig {
    @Optional
    private String preset;
    @Optional
    private String configuration;
    @Optional
    private String includeExcludePattern;
    @Optional
    private String excludeFolders;
    @Optional
    private boolean incremental;
    @Optional
    private boolean privateScan;
    @Optional
    private int low;
    @Optional
    private int medium;
    @Optional
    private int high;
    @Optional
    private int critical;
    @Optional
	private boolean avoidDuplicateProjectScans;  
    @Optional
    private boolean isOverrideProjectSetting;
    @Optional
    private boolean enableSastBranching;
    @Optional
    private String masterBranchProjName;
    @Optional
    private int branchTimeout;
    
    public SastConfig() {
    }

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getIncludeExcludePattern() {
        return includeExcludePattern;
    }

    public void setIncludeExcludePattern(String includeExcludePattern) {
        this.includeExcludePattern = includeExcludePattern;
    }

    public String getExcludeFolders() {
        return excludeFolders;
    }

    public void setExcludeFolders(String excludeFolders) {
        this.excludeFolders = excludeFolders;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public int getLow() {
        return low;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public int getMedium() {
        return medium;
    }

    public void setMedium(int medium) {
        this.medium = medium;
    }

    public int getHigh() {
        return high;
    }

    public void setHigh(int high) {
        this.high = high;
    }
    
    public int getCritical() {
        return critical;
    }

    public void setCritical(int critical) {
        this.critical = critical;
    }

    public boolean isPrivateScan() {
        return privateScan;
    }

    public void setPrivateScan(boolean privateScan) {
        this.privateScan = privateScan;
    }
    
    public boolean isAvoidDuplicateProjectScans() {
		return avoidDuplicateProjectScans;
	}

	public void setAvoidDuplicateProjectScans(boolean avoidDuplicateProjectScans) {
		this.avoidDuplicateProjectScans = avoidDuplicateProjectScans;
	}

	public boolean isOverrideProjectSetting() {
		return isOverrideProjectSetting;
	}
	
	public void setOverrideProjectSetting(boolean isOverrideProjectSetting) {
		this.isOverrideProjectSetting = isOverrideProjectSetting;
	}
	
	public boolean isEnableSASTBranching() {
		return enableSastBranching;
	}

	public void setEnableSASTBranching(boolean enableSASTBranching) {
		this.enableSastBranching = enableSASTBranching;
	}

	public String getMasterBranchProjName() {
		return masterBranchProjName;
	}

	public void setMasterBranchProjName(String masterBranchProjName) {
		this.masterBranchProjName = masterBranchProjName;
	}

	public void setBranchTimeout(int branchTimeout) {
		this.branchTimeout = branchTimeout;
	}
	
	public int getBranchTimeout(){
		return branchTimeout;
	}
    
}
