package com.viaversion.viaversion.protocol;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.ServerProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.SortedSet;

public class ServerProtocolVersionSingleton implements ServerProtocolVersion {
    private final ProtocolVersion protocolVersion;

    public ServerProtocolVersionSingleton(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public ProtocolVersion lowestSupportedProtocolVersion() {
        return this.protocolVersion;
    }

    @Override
    public ProtocolVersion highestSupportedProtocolVersion() {
        return this.protocolVersion;
    }

    @Override
    public SortedSet<ProtocolVersion> supportedProtocolVersions() {
        SortedSet<ProtocolVersion> set = new ObjectLinkedOpenHashSet<>();
        set.add(this.protocolVersion);
        return set;
    }
}
