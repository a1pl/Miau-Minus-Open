package com.viaversion.viaversion.api.platform;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;

public interface ProtocolDetectorService {
    ProtocolVersion serverProtocolVersion(String var1);

    void probeAllServers();

    void setProtocolVersion(String var1, int var2);

    int uncacheProtocolVersion(String var1);

    Object2IntMap<String> detectedProtocolVersions();
}
