package com.viaversion.viaversion.api.protocol.version;

import java.util.Set;

public interface BlockedProtocolVersions {
    boolean contains(ProtocolVersion var1);

    ProtocolVersion blocksBelow();

    ProtocolVersion blocksAbove();

    Set<ProtocolVersion> singleBlockedVersions();
}
