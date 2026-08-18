package com.viaversion.viaversion.dump;

import com.viaversion.viaversion.api.protocol.version.VersionType;
import java.util.Set;

public class VersionInfo {
    private final String javaVersion;
    private final String operatingSystem;
    private final VersionType versionType;
    private final int serverProtocol;
    private final String serverVersion;
    private final Set<String> enabledVersions;
    private final String platformName;
    private final String platformVersion;
    private final String pluginVersion;
    private final String implementationVersion;
    private final Set<String> subPlatforms;

    public VersionInfo(
        String javaVersion,
        String operatingSystem,
        VersionType versionType,
        int serverProtocol,
        String serverVersion,
        Set<String> enabledVersions,
        String platformName,
        String platformVersion,
        String pluginVersion,
        String implementationVersion,
        Set<String> subPlatforms
    ) {
        this.javaVersion = javaVersion;
        this.operatingSystem = operatingSystem;
        this.serverProtocol = serverProtocol;
        this.versionType = versionType;
        this.serverVersion = serverVersion;
        this.enabledVersions = enabledVersions;
        this.platformName = platformName;
        this.platformVersion = platformVersion;
        this.pluginVersion = pluginVersion;
        this.implementationVersion = implementationVersion;
        this.subPlatforms = subPlatforms;
    }

    public String getJavaVersion() {
        return this.javaVersion;
    }

    public String getOperatingSystem() {
        return this.operatingSystem;
    }

    public VersionType getVersionType() {
        return this.versionType;
    }

    public int getServerProtocol() {
        return this.serverProtocol;
    }

    public String getServerVersion() {
        return this.serverVersion;
    }

    public Set<String> getEnabledVersions() {
        return this.enabledVersions;
    }

    public String getPlatformName() {
        return this.platformName;
    }

    public String getPlatformVersion() {
        return this.platformVersion;
    }

    public String getPluginVersion() {
        return this.pluginVersion;
    }

    public String getImplementationVersion() {
        return this.implementationVersion;
    }

    public Set<String> getSubPlatforms() {
        return this.subPlatforms;
    }
}
