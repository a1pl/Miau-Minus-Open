package com.viaversion.viaversion.api.protocol;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

public interface ProtocolPathEntry {
    ProtocolVersion outputProtocolVersion();

    Protocol<?, ?, ?, ?> protocol();
}
