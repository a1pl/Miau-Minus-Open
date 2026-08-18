package com.viaversion.viaversion.protocol;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.ServerProtocolVersion;
import java.util.SortedSet;

public class ServerProtocolVersionRange implements ServerProtocolVersion {
    private final ProtocolVersion lowestSupportedVersion;
    private final ProtocolVersion highestSupportedVersion;
    private final SortedSet<ProtocolVersion> supportedVersions;

    public ServerProtocolVersionRange(
        ProtocolVersion lowestSupportedVersion,
        ProtocolVersion highestSupportedVersion,
        SortedSet<ProtocolVersion> supportedVersions
    ) {
        this.lowestSupportedVersion = lowestSupportedVersion;
        this.highestSupportedVersion = highestSupportedVersion;
        this.supportedVersions = supportedVersions;
    }

    @Override
    public ProtocolVersion lowestSupportedProtocolVersion() {
        return this.lowestSupportedVersion;
    }

    @Override
    public ProtocolVersion highestSupportedProtocolVersion() {
        return this.highestSupportedVersion;
    }

    @Override
    public SortedSet<ProtocolVersion> supportedProtocolVersions() {
        return this.supportedVersions;
    }
}
