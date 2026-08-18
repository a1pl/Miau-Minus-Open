package com.viaversion.viaversion.protocol;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.SubVersionRange;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import java.util.Comparator;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RedirectProtocolVersion extends ProtocolVersion {
    private final ProtocolVersion origin;

    public RedirectProtocolVersion(int version, String name, ProtocolVersion origin) {
        this(version, -1, name, null, origin);
    }

    public RedirectProtocolVersion(
        int version, int snapshotVersion, String name, @Nullable SubVersionRange versionRange, ProtocolVersion origin
    ) {
        super(VersionType.SPECIAL, version, snapshotVersion, name, versionRange);
        this.origin = origin;
    }

    @Override
    protected @Nullable Comparator<ProtocolVersion> customComparator() {
        return (o1, o2) -> {
            if (o1 == this) {
                o1 = this.origin;
            }

            if (o2 == this) {
                o2 = this.origin;
            }

            return o1.compareTo(o2);
        };
    }
}
