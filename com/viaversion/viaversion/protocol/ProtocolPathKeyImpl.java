package com.viaversion.viaversion.protocol;

import com.viaversion.viaversion.api.protocol.ProtocolPathKey;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Objects;

public class ProtocolPathKeyImpl implements ProtocolPathKey {
    private final ProtocolVersion clientProtocolVersion;
    private final ProtocolVersion serverProtocolVersion;

    public ProtocolPathKeyImpl(ProtocolVersion clientProtocolVersion, ProtocolVersion serverProtocolVersion) {
        this.clientProtocolVersion = clientProtocolVersion;
        this.serverProtocolVersion = serverProtocolVersion;
    }

    @Override
    public ProtocolVersion clientProtocolVersion() {
        return this.clientProtocolVersion;
    }

    @Override
    public ProtocolVersion serverProtocolVersion() {
        return this.serverProtocolVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ProtocolPathKeyImpl that = (ProtocolPathKeyImpl)o;
            return this.clientProtocolVersion != that.clientProtocolVersion
                ? false
                : this.serverProtocolVersion == that.serverProtocolVersion;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.clientProtocolVersion, this.serverProtocolVersion);
    }
}
