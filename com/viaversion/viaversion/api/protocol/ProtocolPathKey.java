package com.viaversion.viaversion.api.protocol;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

public interface ProtocolPathKey {
    ProtocolVersion clientProtocolVersion();

    ProtocolVersion serverProtocolVersion();
}
