package com.viaversion.viaversion.protocol;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Objects;

public class ProtocolPathEntryImpl implements ProtocolPathEntry {
    private final ProtocolVersion outputProtocolVersion;
    private final Protocol<?, ?, ?, ?> protocol;

    public ProtocolPathEntryImpl(ProtocolVersion outputProtocolVersion, Protocol<?, ?, ?, ?> protocol) {
        this.outputProtocolVersion = outputProtocolVersion;
        this.protocol = protocol;
    }

    @Override
    public ProtocolVersion outputProtocolVersion() {
        return this.outputProtocolVersion;
    }

    @Override
    public Protocol<?, ?, ?, ?> protocol() {
        return this.protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ProtocolPathEntryImpl that = (ProtocolPathEntryImpl)o;
            return this.outputProtocolVersion != that.outputProtocolVersion
                ? false
                : this.protocol.equals(that.protocol);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.outputProtocolVersion, this.protocol);
    }

    @Override
    public String toString() {
        return "ProtocolPathEntryImpl{outputProtocolVersion="
            + this.outputProtocolVersion
            + ", protocol="
            + this.protocol
            + '}';
    }
}
