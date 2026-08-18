package com.viaversion.viaversion.api.protocol.version;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ProtocolVersion implements Comparable<ProtocolVersion> {
    private static final Map<VersionType, Int2ObjectMap<ProtocolVersion>> VERSIONS = new EnumMap<>(VersionType.class);
    private static final List<ProtocolVersion> VERSION_LIST = new ArrayList<>();
    public static final ProtocolVersion v1_7_2 = register(4, "1.7.2-1.7.5", new SubVersionRange("1.7", 2, 5));
    @Deprecated
    public static final ProtocolVersion v1_7_1 = v1_7_2;
    public static final ProtocolVersion v1_7_6 = register(5, "1.7.6-1.7.10", new SubVersionRange("1.7", 6, 10));
    public static final ProtocolVersion v1_8 = register(47, "1.8.x", new SubVersionRange("1.8", 0, 9));
    public static final ProtocolVersion v1_9 = register(107, "1.9");
    public static final ProtocolVersion v1_9_1 = register(108, "1.9.1");
    public static final ProtocolVersion v1_9_2 = register(109, "1.9.2");
    public static final ProtocolVersion v1_9_3 = register(110, "1.9.3-1.9.4", new SubVersionRange("1.9", 3, 4));
    public static final ProtocolVersion v1_10 = register(210, "1.10.x", new SubVersionRange("1.10", 0, 2));
    public static final ProtocolVersion v1_11 = register(315, "1.11");
    public static final ProtocolVersion v1_11_1 = register(316, "1.11.1-1.11.2", new SubVersionRange("1.11", 1, 2));
    public static final ProtocolVersion v1_12 = register(335, "1.12");
    public static final ProtocolVersion v1_12_1 = register(338, "1.12.1");
    public static final ProtocolVersion v1_12_2 = register(340, "1.12.2");
    public static final ProtocolVersion v1_13 = register(393, "1.13");
    public static final ProtocolVersion v1_13_1 = register(401, "1.13.1");
    public static final ProtocolVersion v1_13_2 = register(404, "1.13.2");
    public static final ProtocolVersion v1_14 = register(477, "1.14");
    public static final ProtocolVersion v1_14_1 = register(480, "1.14.1");
    public static final ProtocolVersion v1_14_2 = register(485, "1.14.2");
    public static final ProtocolVersion v1_14_3 = register(490, "1.14.3");
    public static final ProtocolVersion v1_14_4 = register(498, "1.14.4");
    public static final ProtocolVersion v1_15 = register(573, "1.15");
    public static final ProtocolVersion v1_15_1 = register(575, "1.15.1");
    public static final ProtocolVersion v1_15_2 = register(578, "1.15.2");
    public static final ProtocolVersion v1_16 = register(735, "1.16");
    public static final ProtocolVersion v1_16_1 = register(736, "1.16.1");
    public static final ProtocolVersion v1_16_2 = register(751, "1.16.2");
    public static final ProtocolVersion v1_16_3 = register(753, "1.16.3");
    public static final ProtocolVersion v1_16_4 = register(754, "1.16.4-1.16.5", new SubVersionRange("1.16", 4, 5));
    public static final ProtocolVersion v1_17 = register(755, "1.17");
    public static final ProtocolVersion v1_17_1 = register(756, "1.17.1");
    public static final ProtocolVersion v1_18 = register(757, "1.18-1.18.1", new SubVersionRange("1.18", 0, 1));
    public static final ProtocolVersion v1_18_2 = register(758, "1.18.2");
    public static final ProtocolVersion v1_19 = register(759, "1.19");
    public static final ProtocolVersion v1_19_1 = register(760, "1.19.1-1.19.2", new SubVersionRange("1.19", 1, 2));
    public static final ProtocolVersion v1_19_3 = register(761, "1.19.3");
    public static final ProtocolVersion v1_19_4 = register(762, "1.19.4");
    public static final ProtocolVersion v1_20 = register(763, "1.20-1.20.1", new SubVersionRange("1.20", 0, 1));
    public static final ProtocolVersion v1_20_2 = register(764, "1.20.2");
    public static final ProtocolVersion v1_20_3 = register(765, "1.20.3-1.20.4", new SubVersionRange("1.20", 3, 4));
    public static final ProtocolVersion v1_20_5 = register(766, "1.20.5-1.20.6", new SubVersionRange("1.20", 5, 6));
    public static final ProtocolVersion unknown = new ProtocolVersion(VersionType.SPECIAL, -1, -1, "UNKNOWN", null);
    private final VersionType versionType;
    private final int version;
    private final int snapshotVersion;
    private final String name;
    private final Set<String> includedVersions;

    public static ProtocolVersion register(int version, String name) {
        return register(version, -1, name);
    }

    public static ProtocolVersion register(int version, int snapshotVersion, String name) {
        ProtocolVersion protocolVersion = new ProtocolVersion(VersionType.RELEASE, version, snapshotVersion, name, null);
        register(protocolVersion);
        return protocolVersion;
    }

    public static ProtocolVersion register(int version, String name, @Nullable SubVersionRange versionRange) {
        ProtocolVersion protocolVersion = new ProtocolVersion(VersionType.RELEASE, version, -1, name, versionRange);
        register(protocolVersion);
        return protocolVersion;
    }

    public static void register(ProtocolVersion protocolVersion) {
        VERSION_LIST.add(protocolVersion);
        VERSION_LIST.sort(ProtocolVersion::compareTo);
        Int2ObjectMap<ProtocolVersion> versions = VERSIONS.computeIfAbsent(
            protocolVersion.versionType, $ -> new Int2ObjectOpenHashMap<>()
        );
        versions.put(protocolVersion.version, protocolVersion);
        if (protocolVersion.isSnapshot()) {
            versions.put(protocolVersion.getFullSnapshotVersion(), protocolVersion);
        }
    }

    public static boolean isRegistered(VersionType versionType, int version) {
        Int2ObjectMap<ProtocolVersion> versions = VERSIONS.get(versionType);
        return versions != null && versions.containsKey(version);
    }

    public static boolean isRegistered(int version) {
        return isRegistered(VersionType.RELEASE, version);
    }

    public static @NonNull ProtocolVersion getProtocol(VersionType versionType, int version) {
        Int2ObjectMap<ProtocolVersion> versions = VERSIONS.get(versionType);
        if (versions != null) {
            ProtocolVersion protocolVersion = versions.get(version);
            if (protocolVersion != null) {
                return protocolVersion;
            }
        }

        return new ProtocolVersion(VersionType.SPECIAL, version, -1, "Unknown (" + version + ")", null);
    }

    public static @NonNull ProtocolVersion getProtocol(int version) {
        return getProtocol(VersionType.RELEASE, version);
    }

    @Deprecated
    public static int getIndex(ProtocolVersion version) {
        return VERSION_LIST.indexOf(version);
    }

    public static List<ProtocolVersion> getProtocols() {
        return Collections.unmodifiableList(VERSION_LIST);
    }

    public static @Nullable ProtocolVersion getClosest(String protocol) {
        for (ProtocolVersion version : VERSION_LIST) {
            String name = version.getName();
            if (name.equals(protocol) || version.isRange() && version.getIncludedVersions().contains(protocol)) {
                return version;
            }
        }

        return null;
    }

    @Deprecated
    public ProtocolVersion(int version, String name) {
        this(version, -1, name, null);
    }

    @Deprecated
    public ProtocolVersion(int version, int snapshotVersion, String name, @Nullable SubVersionRange versionRange) {
        this(VersionType.RELEASE, version, snapshotVersion, name, versionRange);
    }

    public ProtocolVersion(
        VersionType versionType, int version, int snapshotVersion, String name, @Nullable SubVersionRange versionRange
    ) {
        this.versionType = versionType;
        this.version = version;
        this.snapshotVersion = snapshotVersion;
        this.name = name;
        Preconditions.checkArgument(
            !this.isVersionWildcard() || versionRange != null, "A wildcard name must have a version range"
        );
        if (versionRange != null) {
            this.includedVersions = new LinkedHashSet<>();

            for (int i = versionRange.rangeFrom(); i <= versionRange.rangeTo(); i++) {
                if (i == 0) {
                    this.includedVersions.add(versionRange.baseVersion());
                }

                this.includedVersions.add(versionRange.baseVersion() + "." + i);
            }
        } else {
            this.includedVersions = Collections.singleton(name);
        }
    }

    public VersionType getVersionType() {
        return this.versionType;
    }

    public int getVersion() {
        return this.version;
    }

    public int getSnapshotVersion() {
        Preconditions.checkArgument(this.isSnapshot());
        return this.snapshotVersion;
    }

    public int getFullSnapshotVersion() {
        Preconditions.checkArgument(this.isSnapshot());
        return 1073741824 | this.snapshotVersion;
    }

    public int getOriginalVersion() {
        return this.snapshotVersion == -1 ? this.version : 1073741824 | this.snapshotVersion;
    }

    public boolean isKnown() {
        return this.version != -1;
    }

    public boolean isRange() {
        return this.includedVersions.size() != 1;
    }

    public Set<String> getIncludedVersions() {
        return Collections.unmodifiableSet(this.includedVersions);
    }

    public boolean isVersionWildcard() {
        return this.name.endsWith(".x");
    }

    public String getName() {
        return this.name;
    }

    public boolean isSnapshot() {
        return this.snapshotVersion != -1;
    }

    public boolean equalTo(ProtocolVersion other) {
        return this.compareTo(other) == 0;
    }

    public boolean newerThan(ProtocolVersion other) {
        return this.compareTo(other) > 0;
    }

    public boolean newerThanOrEqualTo(ProtocolVersion other) {
        return this.compareTo(other) >= 0;
    }

    public boolean olderThan(ProtocolVersion other) {
        return this.compareTo(other) < 0;
    }

    public boolean olderThanOrEqualTo(ProtocolVersion other) {
        return this.compareTo(other) <= 0;
    }

    public boolean betweenInclusive(ProtocolVersion min, ProtocolVersion max) {
        return this.newerThanOrEqualTo(min) && this.olderThanOrEqualTo(max);
    }

    public boolean betweenExclusive(ProtocolVersion min, ProtocolVersion max) {
        return this.newerThan(min) && this.olderThan(max);
    }

    protected @Nullable Comparator<ProtocolVersion> customComparator() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ProtocolVersion that = (ProtocolVersion)o;
            return this.version == that.version
                && this.versionType == that.versionType
                && this.snapshotVersion == that.snapshotVersion;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = this.versionType.hashCode();
        result = 31 * result + this.version;
        return 31 * result + this.snapshotVersion;
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", this.name, this.version);
    }

    public int compareTo(ProtocolVersion other) {
        if (this.versionType == VersionType.SPECIAL && this.customComparator() != null) {
            return this.customComparator().compare(this, other);
        } else if (other.versionType == VersionType.SPECIAL && other.customComparator() != null) {
            return other.customComparator().compare(this, other);
        } else if (this.versionType != other.versionType) {
            return this.versionType.ordinal() < other.versionType.ordinal() ? -1 : 1;
        } else if (this.version != other.version) {
            return this.version < other.version ? -1 : 1;
        } else {
            return Integer.compare(this.snapshotVersion, other.snapshotVersion);
        }
    }
}
