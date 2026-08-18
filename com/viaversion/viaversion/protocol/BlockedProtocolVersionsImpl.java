package com.viaversion.viaversion.protocol;

import com.viaversion.viaversion.api.protocol.version.BlockedProtocolVersions;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Set;

public class BlockedProtocolVersionsImpl implements BlockedProtocolVersions {
    private final Set<ProtocolVersion> singleBlockedVersions;
    private final ProtocolVersion blocksBelow;
    private final ProtocolVersion blocksAbove;

    public BlockedProtocolVersionsImpl(
        Set<ProtocolVersion> singleBlockedVersions, ProtocolVersion blocksBelow, ProtocolVersion blocksAbove
    ) {
        this.singleBlockedVersions = singleBlockedVersions;
        this.blocksBelow = blocksBelow;
        this.blocksAbove = blocksAbove;
    }

    @Override
    public boolean contains(ProtocolVersion protocolVersion) {
        return this.blocksBelow.isKnown() && protocolVersion.olderThan(this.blocksBelow)
            || this.blocksAbove.isKnown() && protocolVersion.newerThan(this.blocksAbove)
            || this.singleBlockedVersions.contains(protocolVersion);
    }

    @Override
    public ProtocolVersion blocksBelow() {
        return this.blocksBelow;
    }

    @Override
    public ProtocolVersion blocksAbove() {
        return this.blocksAbove;
    }

    @Override
    public Set<ProtocolVersion> singleBlockedVersions() {
        return this.singleBlockedVersions;
    }
}
