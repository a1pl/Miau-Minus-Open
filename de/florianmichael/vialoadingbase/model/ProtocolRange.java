package de.florianmichael.vialoadingbase.model;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

@Deprecated
public class ProtocolRange {
    private final ProtocolVersion lowerBound;
    private final ProtocolVersion upperBound;

    public ProtocolRange(ProtocolVersion lowerBound, ProtocolVersion upperBound) {
        if (lowerBound == null && upperBound == null) {
            throw new RuntimeException("Invalid protocol range");
        }

        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public static ProtocolRange andNewer(ProtocolVersion version) {
        return new ProtocolRange(null, version);
    }

    public static ProtocolRange singleton(ProtocolVersion version) {
        return new ProtocolRange(version, version);
    }

    public static ProtocolRange andOlder(ProtocolVersion version) {
        return new ProtocolRange(version, null);
    }

    public boolean contains(ProtocolVersion protocolVersion) {
        return this.lowerBound != null && protocolVersion.olderThan(this.lowerBound)
            ? false
            : this.upperBound == null || protocolVersion.olderThanOrEqualTo(this.upperBound);
    }

    @Override
    public String toString() {
        if (this.lowerBound == null) {
            return this.upperBound.getName() + "+";
        } else if (this.upperBound == null) {
            return this.lowerBound.getName() + "-";
        } else {
            return this.lowerBound == this.upperBound
                ? this.lowerBound.getName()
                : this.lowerBound.getName() + " - " + this.upperBound.getName();
        }
    }
}
