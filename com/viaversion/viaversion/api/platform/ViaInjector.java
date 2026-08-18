package com.viaversion.viaversion.api.platform;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectLinkedOpenHashSet;
import com.viaversion.viaversion.libs.gson.JsonObject;
import java.util.SortedSet;

public interface ViaInjector {
    void inject() throws Exception;

    void uninject() throws Exception;

    default boolean lateProtocolVersionSetting() {
        return false;
    }

    ProtocolVersion getServerProtocolVersion() throws Exception;

    default SortedSet<ProtocolVersion> getServerProtocolVersions() throws Exception {
        SortedSet<ProtocolVersion> versions = new ObjectLinkedOpenHashSet<>();
        versions.add(this.getServerProtocolVersion());
        return versions;
    }

    default String getEncoderName() {
        return "via-encoder";
    }

    default String getDecoderName() {
        return "via-decoder";
    }

    JsonObject getDump();
}
